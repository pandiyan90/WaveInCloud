/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.communication.json;

/**
 * Utility for converting from proto ints to compiler generated enums.
 * It is separated out to avoid excessive download size from generated code.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class ProtoEnums {

  public interface HasIntValue {
    int getValue();
  }

  public static <T extends Enum<T> & HasIntValue> T valOf(int value, T[] enumValues) {
    for (T val : enumValues) {
      if (val.getValue() == value) {
        return val;
      }
    }

    // Return the UNKNOWN value, which is always generated as the last value.
    return enumValues[enumValues.length - 1];
  }

  private ProtoEnums() {}
}
