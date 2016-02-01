/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.message.service;

import org.ros.internal.message.MessageInterfaceClassProvider;
import org.ros.internal.message.RawMessage;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ServiceRequestMessageInterfaceClassProvider implements MessageInterfaceClassProvider {

  @SuppressWarnings("unchecked")
  
  public <T> Class<T> get(String messageType) {
    try {
      String className = messageType.replace("/", ".") + "$Request";
      return (Class<T>) getClass().getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      return (Class<T>) RawMessage.class;
    }
  }
}
