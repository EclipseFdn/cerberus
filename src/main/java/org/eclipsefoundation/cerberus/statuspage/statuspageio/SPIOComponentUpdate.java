/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.statuspage.statuspageio;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import org.eclipsefoundation.cerberus.component.Component.Status;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class SPIOComponentUpdate {
  
  @Nullable
  public abstract Component component();

  public static SPIOComponentUpdate updateWithStatus(SPIOComponent component, Status status) {
    return builder()
      .component(
        Component.builder()
        .groupId(component.groupId())
        .name(component.name())
        .description(component.description())
        .status(status)
        .onlyShowIfDegraded(component.onlyShowIfDegraded())
        .showcase(component.showcase())
        .build()
      ).build();
  }

  public static Builder builder() {
    return new AutoValue_SPIOComponentUpdate.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder component(Component component);
    public abstract SPIOComponentUpdate build();
  }

  @JsonClass(generateAdapter = true, generator = "avm")
  @AutoValue
  public static abstract class Component {
    @Json(name = "group_id")
    @Nullable
    public abstract String groupId();
  
    @Nullable
    public abstract String name();
    
    @Nullable
    public abstract String description();
    
    @Nullable
    public abstract Status status();
    
    public abstract boolean showcase();
    
    @Json(name = "only_show_if_degraded")
    public abstract boolean onlyShowIfDegraded();

    public static Builder builder() {
      return new AutoValue_SPIOComponentUpdate_Component.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
      public abstract Builder groupId(String groupId);
      public abstract Builder name(String name);
      public abstract Builder description(String description);
      public abstract Builder status(Status status);
      public abstract Builder showcase(boolean showcase);
      public abstract Builder onlyShowIfDegraded(boolean onlyShowIfDegraded);
      public abstract Component build();
    }
  }
}