/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.statuspage.statuspageio;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

interface SPIOComponentService {

  @GET("pages/{page_id}/components")
  Call<List<SPIOComponent>> listComponents(@Path("page_id") String pageId);

  @PATCH("pages/{page_id}/components/{component_id}")
  Call<SPIOComponent> updateComponent(@Path("page_id") String pageId, @Path("component_id") String componentId, @Body SPIOComponentUpdate component);

}