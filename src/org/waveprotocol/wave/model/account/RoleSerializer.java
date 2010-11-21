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

package org.waveprotocol.wave.model.account;

import org.waveprotocol.wave.model.util.Serializer;

/**
 *
 */
public class RoleSerializer { 
  
  public static final Serializer<Role> INSTANCE = new Serializer<Role>() {
    @Override
    public Role fromString(String s) {
      return Role.valueOf(s);
    }

    @Override
    public Role fromString(String s, Role defaultValue) {
      if (s != null) {
        return fromString(s);
      }
      return defaultValue;
    }

    @Override
    public String toString(Role x) {
      return x == null ? null : x.toString();
    }
  };
  
  private RoleSerializer() { }
}