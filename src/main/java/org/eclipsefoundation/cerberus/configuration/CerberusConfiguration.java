/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.configuration;

import java.time.Duration;
import java.util.List;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import org.eclipsefoundation.cerberus.configuration.CerberusConfiguration.Monitor.HttpStatus;

@AutoValue
@JsonClass(generateAdapter = true, generator = "avm")
public abstract class CerberusConfiguration {

  @Json(name = "status_pages")
  @Nullable
  public abstract StatusPages statusPages();

  @Nullable
  public abstract Monitor  monitors();

  @Json(name = "default_configuration")
  @Nullable
  public abstract DefaultConfiguration  defaultConfiguration();

  public abstract Builder toBuilder();
  public static Builder builder() {
    return new AutoValue_CerberusConfiguration.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder statusPages(StatusPages statusPages);
    public abstract Builder monitors(Monitor monitor);
    public abstract Builder defaultConfiguration(DefaultConfiguration defaultConfiguration);
    public abstract CerberusConfiguration build();
  }

  @AutoValue
  @JsonClass(generateAdapter = true, generator = "avm")
  public static abstract class StatusPages {
    @Json(name = "statuspage.io")
    @Nullable
    public abstract List<StatusPageIO> statusPageIO();

    @AutoValue
    @JsonClass(generateAdapter = true, generator = "avm")
    public static abstract class StatusPageIO {
      @Nullable
      public abstract String url();
      
      @Json(name = "page_id")
      @Nullable
      public abstract String pageId();
      
      @Nullable
      public abstract String token();
      
      @Json(name = "fetch_rate")
      @Nullable
      public abstract Duration fetchRate();
    }
  }

  @AutoValue
  @JsonClass(generateAdapter = true, generator = "avm")
  public static abstract class Monitor {
    @Json(name = "http_status")
    @Nullable
    public abstract List<HttpStatus> httpStatus();

    @AutoValue
    @JsonClass(generateAdapter = true, generator = "avm")
    public static abstract class HttpStatus implements MonitorConfiguration {
      @Json(name = "component_name")
      @Nullable
      public abstract String componentName();

      @Nullable
      public abstract String target();

      @Nullable
      public abstract String method();

      @Json(name = "status_code_min")
      @Nullable
      public abstract Integer statusCodeMin();

      @Json(name = "status_code_max")
      @Nullable
      public abstract Integer statusCodeMax();

      @Nullable
      public abstract Duration timeout();

      @Json(name = "monitoring_history")
      @Nullable
      public abstract Duration monitoringHistory();

      @Json(name = "initial_delay")
      @Nullable
      public abstract Duration initialDelay();

      @Json(name = "period")
      @Nullable
      public abstract Duration period();

      @Json(name = "anomalies_detection")
      @Nullable
      public abstract AnomaliesDetection anomaliesDetection();

      @AutoValue
      @JsonClass(generateAdapter = true, generator = "avm")
      public static abstract class AnomaliesDetection implements MonitorConfiguration.AnomaliesDetectionConfiguration {
        @Json(name = "degraded_performance_threshold")
        @Nullable
        public abstract Integer degradedPerformanceThreshold();
        
        @Json(name = "partial_outage_threshold")
        @Nullable
        public abstract Integer partialOutageThreshold();
        
        @Json(name = "major_outage_threshold")
        @Nullable
        public abstract Integer majorOutageThreshold();
        
        @Json(name = "period")
        @Nullable
        public abstract Duration period();

        @Json(name = "initial_delay")
        @Nullable
        public abstract Duration initialDelay();

        public abstract Builder toBuilder();

        public static Builder builder() {
          return new AutoValue_CerberusConfiguration_Monitor_HttpStatus_AnomaliesDetection.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
          public abstract Builder degradedPerformanceThreshold(Integer degradedPerformanceThreshold);
          public abstract Builder partialOutageThreshold(Integer partialOutageThreshold);
          public abstract Builder majorOutageThreshold(Integer majorOutageThreshold);
          public abstract Builder period(Duration period);
          public abstract Builder initialDelay(Duration initialDelay);
          public abstract AnomaliesDetection build();
        }

        public AnomaliesDetection withDefault(DefaultConfiguration defaultConfiguration) {
          Builder builder = defaultConfiguration.httpStatus().anomaliesDetection().toBuilder();
          if (degradedPerformanceThreshold() != null)
            builder.degradedPerformanceThreshold(degradedPerformanceThreshold());
          if (partialOutageThreshold() != null)
            builder.partialOutageThreshold(partialOutageThreshold());
          if (majorOutageThreshold() != null)
            builder.majorOutageThreshold(majorOutageThreshold());
          if (period() != null)
            builder.period(period());
          if (initialDelay() != null)
            builder.initialDelay(initialDelay());
          return builder.build();
        }
      }

      public abstract Builder toBuilder();

      public static Builder builder() {
        return new AutoValue_CerberusConfiguration_Monitor_HttpStatus.Builder();
      }

      @AutoValue.Builder
      public static abstract class Builder {
        public abstract Builder componentName(String componentName);
        public abstract Builder target(String target);
        public abstract Builder method(String method);
        public abstract Builder statusCodeMin(Integer statusCodeMin);
        public abstract Builder statusCodeMax(Integer statusCodeMax);
        public abstract Builder timeout(Duration timeout);
        public abstract Builder monitoringHistory(Duration monitoringHistory);
        public abstract Builder initialDelay(Duration initialDelay);
        public abstract Builder period(Duration period);
        public abstract Builder anomaliesDetection(AnomaliesDetection anomaliesDetection);
        public abstract HttpStatus build();
      }

      public HttpStatus withDefault(DefaultConfiguration defaultConfiguration) {
        Builder builder = defaultConfiguration.httpStatus().toBuilder();
        if (componentName() != null)
          builder.componentName(componentName());
        if (target() != null)
          builder.target(target());
        if (method() != null)
          builder.method(method());
        if (statusCodeMin() != null)
          builder.statusCodeMin(statusCodeMin());
        if (statusCodeMax() != null)
          builder.statusCodeMax(statusCodeMax());
        if (timeout() != null)
          builder.timeout(timeout());
        if (monitoringHistory() != null)
          builder.monitoringHistory(monitoringHistory());
        if (initialDelay() != null)
          builder.initialDelay(initialDelay());
        if (period() != null)
          builder.period(period());
        if (anomaliesDetection() != null)
          builder.anomaliesDetection(anomaliesDetection().withDefault(defaultConfiguration));
        return builder.build();
      }
    }
  }

  @AutoValue
  @JsonClass(generateAdapter = true, generator = "avm")
  public static abstract class DefaultConfiguration {
    @Json(name = "http_status")
    @Nullable
    public abstract HttpStatus httpStatus();
  }
}