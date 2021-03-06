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

package org.waveprotocol.box.server.rpc.render.account;

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * Profile information for a participant.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface Profile {

  /**
   * @return the participant id for this profile
   */
  ParticipantId getParticipantId();

  /**
   * @return the address for this profile, same as {@link #getParticipantId()}
   */
  String getAddress();

  /**
   * @return the participant's full name
   */
  String getFullName();

  /**
   * @return the participant's first name
   */
  String getFirstName();

  /**
   * @return the URL of a participant's avatar image
   */
  String getImageUrl();
}
