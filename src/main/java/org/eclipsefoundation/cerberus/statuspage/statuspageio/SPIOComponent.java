/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.statuspage.statuspageio;

import java.time.Instant;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import org.eclipsefoundation.cerberus.component.Component.Status;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class SPIOComponent {

  @Nullable
  public abstract String id();
  
  @Json(name = "page_id")
  @Nullable
  public abstract String pageId();
  
  @Json(name = "group_id")
  @Nullable
  public abstract String groupId();
  
  @Json(name = "created_at")
  @Nullable
  public abstract Instant createdAt();
  
  @Json(name = "updated_at")
  @Nullable
  public abstract Instant updatedAt();
  
  public abstract boolean group();
  
  @Nullable
  public abstract String name();
  
  @Nullable
  public abstract String description();
  
  public abstract int position();
  
  @Nullable
  public abstract Status status();
  
  public abstract boolean showcase();
  
  @Json(name = "only_show_if_degraded")
  public abstract boolean onlyShowIfDegraded();
  
  @Json(name = "automation_email")
  @Nullable
  public abstract String automationEmail();

  public static Builder builder() {
    return new AutoValue_SPIOComponent.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder id(String id);
    public abstract Builder pageId(String pageId);
    public abstract Builder groupId(String groupId);
    public abstract Builder createdAt(Instant createdAt);
    public abstract Builder updatedAt(Instant updatedAt);
    public abstract Builder group(boolean group);
    public abstract Builder name(String name);
    public abstract Builder description(String description);
    public abstract Builder position(int position);
    public abstract Builder status(Status status);
    public abstract Builder showcase(boolean showcase);
    public abstract Builder onlyShowIfDegraded(boolean onlyShowIfDegraded);
    public abstract Builder automationEmail(String automationEmail);
    public abstract SPIOComponent build();
  }
}