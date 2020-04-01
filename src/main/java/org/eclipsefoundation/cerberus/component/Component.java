/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.component;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Component {

  public static enum Status {
    UNKNOWN,
    OPERATIONAL,
    UNDER_MAINTENANCE,
    DEGRADED_PERFORMANCE,
    PARTIAL_OUTAGE,
    MAJOR_OUTAGE;
  }

  public abstract String name();

  public abstract Status status();

  public static Builder builder() {
    return new AutoValue_Component.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder name(String name);
    public abstract Builder status(Status status);
    public abstract Component build();
  }
}