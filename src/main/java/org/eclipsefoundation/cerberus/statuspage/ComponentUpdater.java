/*******************************************************************************
 * Copyright (c) 2020 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipsefoundation.cerberus.statuspage;

import java.util.List;

import org.eclipsefoundation.cerberus.component.Component;
import org.eclipsefoundation.cerberus.component.Component.Status;

public interface ComponentUpdater {

  List<Component> components();

  void updateStatus(String componentName, Status status);
}