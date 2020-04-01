/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.monitors.http.responsetime;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Report {
  public abstract OffsetDateTime time();
  public abstract Duration responseTime();
  public abstract int statusCode();
  public abstract Optional<Exception> exception();

  public static Report create(OffsetDateTime time, Duration responseTime, int statusCode, Optional<Exception> exception) {
    return new AutoValue_Report(time, responseTime, statusCode, exception);
  }
}