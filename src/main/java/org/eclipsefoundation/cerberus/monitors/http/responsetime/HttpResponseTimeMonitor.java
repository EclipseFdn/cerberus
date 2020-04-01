/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.monitors.http.responsetime;

public class HttpResponseTimeMonitor {

  // private static Logger LOG = LoggerFactory.getLogger(HttpResponseTimeMonitor.class);
  
  // private final Configuration config;

  // private final EvictingQueue<Report> datapoints;
  // /**
  //  * The lock protecting datapoints
  //  */
  // private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  // private final OkHttpClient client;
  // private final ResponseTimeEventListener.Factory factory;

  // public static class HttpResponseTimeMonitorFactory implements Monitor.Factory {
    
  //   private final ConnectionPool connectionPool;

  //   public HttpResponseTimeMonitorFactory(ConnectionPool connectionPool) {
  //     this.connectionPool = connectionPool;
  //   }

  //   public HttpResponseTimeMonitor createMonitor(Configuration config, OkHttpClient client) {
  //     return new HttpResponseTimeMonitor(config, client.newBuilder()
  //       .connectionPool(connectionPool)
  //       .eventListenerFactory(new ResponseTimeEventListener.Factory())
  //       .callTimeout(config.timeout()).build());
  //   }
  // }

  // HttpResponseTimeMonitor(Configuration config, OkHttpClient client) {
  //   this.config = config;
  //   this.client = client;
  //   this.datapoints = null;//EvictingQueue.create(Ints.checkedCast(config.monitoringHistory().dividedBy(config.period())));
  //   this.factory = ((ResponseTimeEventListener.Factory)client.eventListenerFactory());
  // }

  // @Override
  // public MonitorConfiguration configuration() {
  //   return null;//this.config;
  // }
  
  // public void run() {
  //   Request r = new Request.Builder().url(config.url()).get().build();
  //   OffsetDateTime now = OffsetDateTime.now();
  //   Call call = client.newCall(r);
  //   try (Response response = call.execute()) {
  //     response.body().source().readByteString();
  //     ResponseTimeEventListener eventListener = factory.getAndRemove(call);
  //     addDatapoint(now, eventListener.responseTime(), response.code(), Optional.empty());
  //   } catch (IOException e) {
  //     addDatapoint(now, Duration.ZERO, Integer.MIN_VALUE, Optional.of(e));
  //   }
  // }

  // private void addDatapoint(OffsetDateTime time, Duration responseTime, int code, Optional<Exception> exception) {
  //   try {
  //     lock.writeLock().lockInterruptibly();
  //     try {
  //       Report report = Report.create(time, responseTime, code, exception);
  //       LOG.debug(config.url() + " " + report);
  //       datapoints.add(report);
  //     } finally {
  //       lock.writeLock().unlock();;
  //     }
  //   } catch (InterruptedException e) {
  //     Thread.currentThread().interrupt();
  //   }
  // }

  // @Override
  // public void detectAnomalies() {
  //   long[] anomalies = new long[0];
  //   try {
  //     lock.readLock().lockInterruptibly();
  //     try {
  //       anomalies = datapoints.stream()
  //         .filter(Predicate.not(r -> Range.closed(200, 399).contains(r.statusCode())))
  //         .map(Report::responseTime)
  //         .mapToLong(Duration::toMillis)
  //         .toArray();
  //     } finally {
  //       lock.readLock().unlock();
  //     }
  //   } catch (InterruptedException e) {
  //     Thread.currentThread().interrupt();
  //   }

  //   Map<Integer, Double> q = Quantiles.percentiles().indexes(config.outagePercentile()).compute(anomalies);
    
  //   // if (config.partialOutageAnomalyCountRange().contains(anomalyCount)) {
  //   //   LOG.debug(Component.Status.PARTIAL_OUTAGE + " ("  + anomalyCount + " anomalies in the last " + config.monitoringHistory() + ")");
  //   //   return Component.Status.PARTIAL_OUTAGE;
  //   // } else if (config.majorOutageAnomalyCountRange().contains(anomalyCount)) {
  //   //   LOG.debug(Component.Status.MAJOR_OUTAGE + " ("  + anomalyCount + " anomalies in the last " + config.monitoringHistory() + ")");
  //   //   return Component.Status.MAJOR_OUTAGE;
  //   // } else {
  //   //   LOG.debug(Component.Status.OPERATIONAL + " ("  + anomalyCount + " anomalies in the last " + config.monitoringHistory() + ")");
  //   //   return Component.Status.OPERATIONAL;
  //   // }
  // }
}