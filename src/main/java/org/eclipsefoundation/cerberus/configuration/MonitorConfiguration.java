/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.configuration;

import java.time.Duration;

import org.eclipsefoundation.cerberus.component.Component.Status;

public interface MonitorConfiguration {

  String componentName();
  String target();
  
  Duration monitoringHistory();

  Duration initialDelay();
  Duration period();

  AnomaliesDetectionConfiguration anomaliesDetection();

  interface AnomaliesDetectionConfiguration {
    Duration period();
    Duration initialDelay();
  
    Integer degradedPerformanceThreshold();
    Integer partialOutageThreshold();
    Integer majorOutageThreshold();

    default Status statusFromAnomaliesCount(int anomaliesCount) {
      if (anomaliesCount >= majorOutageThreshold()) {
        return Status.MAJOR_OUTAGE;
      } else if (anomaliesCount >= partialOutageThreshold()) {
        return Status.PARTIAL_OUTAGE;
      } else if (anomaliesCount >= degradedPerformanceThreshold()) {
        return Status.DEGRADED_PERFORMANCE;
      } else {
        return Status.OPERATIONAL;
      }
    }
  }
}