/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.monitors;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipsefoundation.cerberus.configuration.MonitorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Monitor {
 
  Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  public static interface Factory {

  }

  public abstract MonitorConfiguration configuration();

  public abstract void run();

  public abstract void detectAnomalies();

  public CompletableFuture<?> schedule(ScheduledExecutorService executor) {
    CompletableFuture<?> runFuture = new CompletableFuture<>();
    executor.scheduleAtFixedRate(() -> {
      try {
        run();
      } catch (Exception exception) {
        LOGGER.error("Error while running monitor {}", configuration().componentName(), exception);
      } catch (Error error) {
        runFuture.completeExceptionally(error);
      }
    }, configuration().initialDelay().toMillis() + new Random().nextInt((int)Duration.ofMinutes(1).toMillis()), configuration().period().toMillis(), TimeUnit.MILLISECONDS);

    CompletableFuture<?> detectAnomaliesFuture = new CompletableFuture<>();
    executor.scheduleWithFixedDelay(() -> {
      try {
        detectAnomalies();
      } catch (Exception exception) {
        LOGGER.error("Error while detecting anomalies of monitor {}", configuration().componentName(), exception);
      } catch (Error error) {
        detectAnomaliesFuture.completeExceptionally(error);
      }
    }, configuration().anomaliesDetection().initialDelay().toMillis() + new Random().nextInt((int)Duration.ofMinutes(1).toMillis()), configuration().anomaliesDetection().period().toMillis(), TimeUnit.MILLISECONDS);
    LOGGER.info("Scheduled {} for component {}", this.getClass().getSimpleName(), configuration().componentName());
    return CompletableFuture.anyOf(runFuture, detectAnomaliesFuture);
  }
}