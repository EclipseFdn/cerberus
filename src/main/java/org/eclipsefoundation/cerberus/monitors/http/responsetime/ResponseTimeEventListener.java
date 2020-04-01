/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.monitors.http.responsetime;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;

class ResponseTimeEventListener extends EventListener {
  private OffsetDateTime startTime;
  private Duration responseTime;
  
  @Override
  public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
    startTime = OffsetDateTime.now();
  }

  @Override
  public void connectionReleased(Call call, Connection connection) {
    responseTime = Duration.between(startTime, OffsetDateTime.now());
  }

  public Duration responseTime() {
    return responseTime;
  }

  public static class Factory implements EventListener.Factory {

    Map<Call, ResponseTimeEventListener> eventListeners = new ConcurrentHashMap<>();

    @Override
    public EventListener create(Call call) {
      ResponseTimeEventListener eventListener = new ResponseTimeEventListener();
      eventListeners.put(call, eventListener);
      return eventListener;
    }

    public ResponseTimeEventListener getAndRemove(Call call) {
      return eventListeners.remove(call);
    }

  }
}
