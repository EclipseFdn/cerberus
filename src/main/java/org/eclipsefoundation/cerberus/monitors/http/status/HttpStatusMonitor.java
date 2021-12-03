/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.monitors.http.status;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

import org.eclipsefoundation.cerberus.component.Component.Status;
import org.eclipsefoundation.cerberus.configuration.CerberusConfiguration;
import org.eclipsefoundation.cerberus.monitors.Monitor;
import org.eclipsefoundation.cerberus.statuspage.ComponentUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpStatusMonitor extends Monitor {

  private static Logger LOGGER = LoggerFactory.getLogger(HttpStatusMonitor.class);

  private final CerberusConfiguration.Monitor.HttpStatus configuration;

  private final Range<Integer> statusCodeRange;

  private final EvictingQueue<Report> datapoints;
  /**
   * The lock protecting datapoints
   */
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private final OkHttpClient client;

  private final List<ComponentUpdater> updaters;

  public static class HttpStatusMonitorFactory implements Monitor.Factory {

    private final ConnectionPool connectionPool;

    public HttpStatusMonitorFactory(ConnectionPool connectionPool) {
      this.connectionPool = connectionPool;
    }

    public HttpStatusMonitor createMonitor(CerberusConfiguration.Monitor.HttpStatus configuration, OkHttpClient client, List<ComponentUpdater> updaters) {
      return new HttpStatusMonitor(configuration, client.newBuilder()
        .connectionPool(connectionPool)
        .connectTimeout(configuration.connectTimeout())
        .readTimeout(configuration.readTimeout())
        .build(), updaters);
    }
  }

  private HttpStatusMonitor(CerberusConfiguration.Monitor.HttpStatus configuration, OkHttpClient client, List<ComponentUpdater> updaters) {
    this.configuration = configuration;
    this.statusCodeRange = Range.closed(configuration.statusCodeMin(), configuration.statusCodeMax());
    this.client = client;
    this.updaters = updaters;
    this.datapoints = EvictingQueue.create(Ints.checkedCast(configuration.monitoringHistory().dividedBy(configuration.period())));
  }

  @Override
  public CerberusConfiguration.Monitor.HttpStatus configuration() {
    return configuration;
  }

  @Override
  public void run() {
    Request r = new Request.Builder().url(configuration.target()).get().build();
    OffsetDateTime now = OffsetDateTime.now();
    try (Response response = client.newCall(r).execute()) {
      response.body().source().readByteString();
      addDatapoint(now, response.code(), Optional.empty());
    } catch (IOException e) {
      LOGGER.error("Exception while monitoring {}", configuration.target(), e);
      addDatapoint(now, Integer.MIN_VALUE, Optional.of(e));
    }
  }

  private void addDatapoint(OffsetDateTime time, int code, Optional<Exception> exception) {
    try {
      lock.writeLock().lockInterruptibly();
      try {
        Report report = Report.create(time, code, exception);
        // The HTTP 429 Too Many Requests response status code indicates the user has sent too many requests in a given amount of time ("rate limiting").
        // consider it a success
        if (!statusCodeRange.contains(code) && code != 429) {
          LOGGER.warn("{} - {}", configuration.target(), report);
        } else {
          LOGGER.debug("{} - {}", configuration.target(), report);
        }
        datapoints.add(report);
      } finally {
        lock.writeLock().unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void detectAnomalies() {
    int anomaliesCount = 0;
    try {
      lock.readLock().lockInterruptibly();
      try {
        anomaliesCount = (int)datapoints.stream()
          .map(Report::statusCode)
          .filter(Predicate.not(statusCodeRange))
          // The HTTP 429 Too Many Requests response status code indicates the user has sent too many requests in a given amount of time ("rate limiting").
          // consider it a success
          .filter(status -> status != 429)
          .count();
      } finally {
        lock.readLock().unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Status newStatus = configuration.anomaliesDetection().statusFromAnomaliesCount(anomaliesCount);
    LOGGER.debug("Component {}: {} ({} anomalies in the last {})", configuration.componentName(), newStatus, anomaliesCount, configuration().monitoringHistory());
    updaters.forEach(u -> u.updateStatus(configuration.componentName(), newStatus));
  }
}