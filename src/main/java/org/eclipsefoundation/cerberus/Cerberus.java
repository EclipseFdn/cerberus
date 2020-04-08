/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import org.eclipsefoundation.cerberus.configuration.CerberusConfiguration;
import org.eclipsefoundation.cerberus.monitors.http.status.HttpStatusMonitor;
import org.eclipsefoundation.cerberus.statuspage.ComponentUpdater;
import org.eclipsefoundation.cerberus.statuspage.statuspageio.SPIOComponentUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSource;
import okio.Okio;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

@Command(name = "cerberus", 
versionProvider = Cerberus.MavenVersion.class, 
mixinStandardHelpOptions = true, 
showDefaultValues = true)
public class Cerberus implements Callable<Integer> {

  private static Logger LOGGER = LoggerFactory.getLogger(Cerberus.class);

  @Option(names = {"-c", "--configuration"}, required = true, description = "Configuration file (JSON)")
  private Path configurationFile;

  @Option(names = {"-s", "--statuspage"}, description = "Status Pages Configuration file (JSON)")
  private Path statusPageConfiguration;

  public static void main(String... args) {
    int exitCode = new CommandLine(new Cerberus()).execute(args);
    System.exit(exitCode);
  }

  public Integer call() throws Exception {
    LOGGER.info("Starting...");
    Moshi moshi = new Moshi.Builder().add(new Object() {
      @FromJson
      Duration fromJson(String date) {
        return Duration.parse(date);
      }

      @ToJson
      String toJson(Duration date) {
        return date.toString();
      }
    }).build();

    CerberusConfiguration configuration;
    JsonAdapter<CerberusConfiguration> configAdapter = moshi.adapter(CerberusConfiguration.class);
    try (BufferedSource configSource = Okio.buffer(Okio.source(Files.newInputStream(configurationFile)))) {
      if (statusPageConfiguration != null) {
        JsonAdapter<CerberusConfiguration.StatusPages> statusPageAdapter = moshi.adapter(CerberusConfiguration.StatusPages.class);
        try (BufferedSource statusPageConfigSource = Okio.buffer(Okio.source(Files.newInputStream(statusPageConfiguration)))) {
          CerberusConfiguration.StatusPages statusPageConfig = statusPageAdapter.fromJson(statusPageConfigSource);
          configuration = configAdapter.fromJson(configSource).toBuilder().statusPages(statusPageConfig).build();
        }
      } else {
        configuration = configAdapter.fromJson(configSource);
      }
    }

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(64, threadFactory("Cerberus-Thread-%d"));

    Dispatcher dispatcher = new Dispatcher(
      new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory("HTTPClient-Thread-%d")));
    HttpLoggingInterceptor loggingInterceptor;
    if (LOGGER.isTraceEnabled()) {
      loggingInterceptor = new HttpLoggingInterceptor((message) -> {
        LOGGER.trace(message);
      });
      loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
    } else {
      loggingInterceptor = new HttpLoggingInterceptor((message) -> {
        LOGGER.debug(message);
      });
      loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
    }
    loggingInterceptor.redactHeader("Authorization");
    OkHttpClient client = new OkHttpClient.Builder()
    .dispatcher(dispatcher)
    .addInterceptor(loggingInterceptor)
    .build();

    LOGGER.info("Initializing status pages...");
    List<ComponentUpdater> updaters = configuration.statusPages().statusPageIO().stream()
      .map(c -> SPIOComponentUpdater.create(executor, client, c.url(), c.pageId(), c.token(), c.fetchRate()))
      .collect(ImmutableList.toImmutableList());
    LOGGER.info("Status pages initialized!");

    ConnectionPool connectionPool = new ConnectionPool(configuration.monitors().httpStatus().size(), 5, TimeUnit.MINUTES);
    HttpStatusMonitor.HttpStatusMonitorFactory factory = new HttpStatusMonitor.HttpStatusMonitorFactory(connectionPool);

    CompletableFuture<?>[] statusMonitors = configuration.monitors().httpStatus().stream()
      .map(m -> m.withDefault(configuration.defaultConfiguration()))
      .map(m -> {
        HttpStatusMonitor monitor1 = factory.createMonitor(m, client, updaters);
        return monitor1.schedule(executor);
      }).toArray(CompletableFuture[]::new);

    CompletableFuture.anyOf(statusMonitors).exceptionally(e -> { 
      LOGGER.error("Fatal error", e.getCause());
      throw new RuntimeException(e);
    }).join();

    return 0;
  }

  static ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder()
    .setNameFormat(nameFormat)
    .setUncaughtExceptionHandler((thread, throwable) -> 
      LOGGER.error("Exception in thread {}.\n{}", thread.getName(), throwable))
    .setDaemon(false)
    .build();
  }

  static class MavenVersion implements IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
      Optional<String> version = Optional.empty();
      ClassLoader cl = Cerberus.class.getClassLoader() == null ? ClassLoader.getSystemClassLoader()
          : Cerberus.class.getClassLoader();
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(cl.getResourceAsStream("version.txt"), StandardCharsets.UTF_8))) {
        version = br.lines().findFirst();
      }
      return new String[] { "Eclipse Foundation Monitoring Tools - Cerberus", version.orElse("<unknown version>"), };
    }
  }
}