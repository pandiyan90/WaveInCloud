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

package org.waveprotocol.wave.examples.fedone.persistence;

import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Interface for the storage and retrieval of {@link AccountData}s.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface AccountStore {

  /**
   * Returns an {@link AccountData} for the given username or null if not
   * exists.
   *
   * @param id participant id of the requested account.
   */
  AccountData getAccount(ParticipantId id);

  /**
   * Puts the given {@link AccountData} in the storage, overrides an existing
   * account if the username is already in use.
   *
   * @param account to store.
   */
  void putAccount(AccountData account);

  /**
   * Removes an account from storage.
   *
   * @param id the participant id of the account to remove.
   */
  void removeAccount(ParticipantId id);
}
