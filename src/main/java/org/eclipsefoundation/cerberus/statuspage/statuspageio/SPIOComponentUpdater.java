/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.statuspage.statuspageio;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipsefoundation.cerberus.component.Component;
import org.eclipsefoundation.cerberus.component.Component.Status;
import org.eclipsefoundation.cerberus.statuspage.ComponentUpdater;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class SPIOComponentUpdater implements ComponentUpdater {

  private static Logger LOGGER = LoggerFactory.getLogger(SPIOComponentUpdater.class);

  private final String pageId;
  private final SPIOComponentService componentService;

  private volatile ImmutableList<SPIOComponent> components;

  SPIOComponentUpdater(String pageId, SPIOComponentService componentService) {
    this.pageId = pageId;
    this.componentService = componentService;
    this.components = ImmutableList.of();
  }

  public static ComponentUpdater create(ScheduledExecutorService executor, OkHttpClient client, String baseUrl, String pageId, String authToken, Duration fetchRate) {
    Moshi moshi = new Moshi.Builder().add(new Object() {
      @FromJson
      Instant fromJson(String date) {
        return Instant.parse(date);
      }

      @ToJson
      String toJson(Instant date) {
        return DateTimeFormatter.ISO_INSTANT.format(date);
      }
    }).add(new Object() {
      @FromJson Status fromJson(String status) {
        switch (status) {
          case "operational": return Status.OPERATIONAL;
          case "under_maintenance": return Status.UNDER_MAINTENANCE;
          case "degraded_performance": return Status.DEGRADED_PERFORMANCE;
          case "partial_outage": return Status.PARTIAL_OUTAGE;
          case "major_outage": return Status.MAJOR_OUTAGE;
          default: return Status.UNKNOWN;
        }
      }
    
      @ToJson String toJson(Status status) {
        switch (status) {
          case UNKNOWN: return "";
          case OPERATIONAL: return "operational";
          case UNDER_MAINTENANCE: return "under_maintenance";
          case DEGRADED_PERFORMANCE: return "degraded_performance";
          case PARTIAL_OUTAGE: return "partial_outage";
          case MAJOR_OUTAGE: return "major_outage";
          default: throw new RuntimeException("Unsuported Status kind " + status);
        }
      }
    }).build();
    MoshiConverterFactory moshiConverterFactory = MoshiConverterFactory.create(moshi);
    OkHttpClient authClient = client.newBuilder().addInterceptor(
        (chain) -> chain.proceed(chain.request().newBuilder().header("Authorization", "OAuth " + authToken).build()))
        .build();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl).client(authClient)
        .addConverterFactory(moshiConverterFactory).build();

    SPIOComponentUpdater ret = new SPIOComponentUpdater(pageId, retrofit.create(SPIOComponentService.class));
    ret.fetchComponents().join();
    executor.scheduleWithFixedDelay(ret::fetchComponents, fetchRate.toMillis(), fetchRate.toMillis(), TimeUnit.MILLISECONDS);
    return ret;
  }
  

  @Override
  public void updateStatus(String componentName, Status status) {
    Optional<SPIOComponent> componentByName = componentByName(componentName);

    if (componentByName.isPresent()) {
      SPIOComponent component = componentByName.get();
      if (component.status() ==  Status.UNDER_MAINTENANCE) {
        LOGGER.info("Component {} is under maintenance, its status won't be updated", componentName);
      } else if (component.status() != status) {
        doUpdateStatus(component, status);
      } else {
        LOGGER.debug("Not necessary to update component {} as its remote and local status are identical ({})", componentName, status);
      }
    } else {
      LOGGER.error("Unable to find remote component with name {}", componentName);
    }
  }

  private void doUpdateStatus(SPIOComponent component, Status status) {
    SPIOComponentUpdate componentUpdate = SPIOComponentUpdate.updateWithStatus(component, status);
    Call<SPIOComponent> call = componentService.updateComponent(pageId, component.id(), componentUpdate);
    call.enqueue(new Callback<SPIOComponent>(){
      @Override
      public void onResponse(Call<SPIOComponent> call, Response<SPIOComponent> response) {
        if (response.isSuccessful()) {
          LOGGER.info("Status of component {} has been changed ({} -> {})", component.name(), component.status(), status);
          fetchComponents();
        } else {
          try {
            LOGGER.error("Unable to update status of component {} (code={}): {}", component.name(), response.code(),
                response.errorBody().string());
          } catch (IOException e) {
            LOGGER.error("Unable to update status of component {} (code={})", component.name(), response.code());
          }
        }
      }
    
      @Override
      public void onFailure(Call<SPIOComponent> call, Throwable t) {
        LOGGER.error("Unable to update status of component {} from {} to {}", component.name(), component.status(), status, t);
      }
    });
  }

  private Optional<SPIOComponent> componentByName(String name) {
    return this.components.stream()
      .filter(c -> name.equals(c.name()))
      .findFirst();
  }

  @Override
  public List<Component> components() {
    return this.components.stream()
      .map(SPIOComponentUpdater::fromSTIOComponent)
      .collect(ImmutableList.toImmutableList());
  }

  private CompletableFuture<List<SPIOComponent>> fetchComponents() {
    CompletableFuture<List<SPIOComponent>> future = new CompletableFuture<>();
    componentService.listComponents(pageId).enqueue(new Callback<List<SPIOComponent>>() {
      @Override
      public void onResponse(Call<List<SPIOComponent>> call, Response<List<SPIOComponent>> response) {
        if (response.isSuccessful()) {
          ImmutableList<SPIOComponent> newComponents = ImmutableList.copyOf(response.body());
          SPIOComponentUpdater.this.components = newComponents;
          future.complete(newComponents);
          LOGGER.debug("{} components have been fetched from statuspage.io {}", components.size(), pageId);
          LOGGER.debug("Components names: {}", components.stream().map(SPIOComponent::name).collect(Collectors.joining(", ")));
        } else {
          try {
            LOGGER.error("Unable to fetch components from statuspage.io {} (code={}): {}", pageId, response.code(), response.errorBody().string());
            future.completeExceptionally(new Exception("Unable to fetch components from statuspage.io"));
          } catch (IOException e) {
            LOGGER.error("Unable to fetch components from statuspage.io {} (code={})", pageId, response.code());
            future.completeExceptionally(new Exception("Unable to fetch components from statuspage.io"));
          }
        }
      }
      @Override
      public void onFailure(Call<List<SPIOComponent>> call, Throwable t) {
        LOGGER.error("Unable to fetch components of page {}", pageId, t);
        future.completeExceptionally(t);
      }
    });
    return future;
  }

  private static Component fromSTIOComponent(SPIOComponent c) {
    return Component.builder()
      .name(c.name())
      .status(c.status())
      .build();
  }
}