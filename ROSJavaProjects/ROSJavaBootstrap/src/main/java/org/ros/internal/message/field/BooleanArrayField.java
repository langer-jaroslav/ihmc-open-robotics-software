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

package org.ros.internal.message.field;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.base.Preconditions;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class BooleanArrayField extends Field {

  private final int size;

  private boolean[] value;

  public static BooleanArrayField newVariable(String name, int size) {
    return new BooleanArrayField(PrimitiveFieldType.BOOL, name, size);
  }

  private BooleanArrayField(FieldType type, String name, int size) {
    super(type, name, false);
    this.size = size;
    setValue(new boolean[Math.max(0, size)]);
  }

  @SuppressWarnings("unchecked")
  
  public boolean[] getValue() {
    return value;
  }

  
  public void setValue(Object value) {
    Preconditions.checkArgument(size < 0 || ((boolean[]) value).length == size);
    this.value = (boolean[]) value;
  }

  
  public void serialize(ChannelBuffer buffer) {
    if (size < 0) {
      buffer.writeInt(value.length);
    }
    for (boolean v : value) {
      type.serialize(v, buffer);
    }
  }

  
  public void deserialize(ChannelBuffer buffer) {
    int currentSize = size;
    if (currentSize < 0) {
      currentSize = buffer.readInt();
    }
    value = new boolean[currentSize];
    for (int i = 0; i < currentSize; i++) {
      value[i] = buffer.readByte() == 1;
    }
  }

  
  public String getMd5String() {
    return String.format("%s %s\n", type, name);
  }

  
  public String getJavaTypeName() {
    return type.getJavaTypeName() + "[]";
  }

  
  public String toString() {
    return "BooleanArrayField<" + type + ", " + name + ">";
  }

  
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    BooleanArrayField other = (BooleanArrayField) obj;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!Arrays.equals(value, other.value))
      return false;
    return true;
  }
}
