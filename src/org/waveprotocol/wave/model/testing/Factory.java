/**
 * Copyright 2008 Google Inc.
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
 */

package org.waveprotocol.wave.model.testing;

/**
 * Generic factory interface.  The intended use within this test package is
 * to allow black-box tests, which only test an interface, to be decoupled from
 * the construction of the particular instance of that interface to test.
 *
 * @param <T> type of created instances
 */
public interface Factory<T> {
  /**
   * Creates an instance.
   */
  T create();
}
