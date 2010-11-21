/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.concurrencycontrol.channel;

import org.waveprotocol.wave.model.id.WaveId;

/**
 * Factory for creating view channels.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ViewChannelFactory {
  /**
   * Creates a view channel.
   *
   * @param waveId wave id for the view
   */
  ViewChannel create(WaveId waveId);
}

