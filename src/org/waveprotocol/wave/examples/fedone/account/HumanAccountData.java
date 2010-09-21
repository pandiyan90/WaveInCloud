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

package org.waveprotocol.wave.examples.fedone.account;

import org.waveprotocol.wave.examples.fedone.authentication.PasswordDigest;

/**
 * {@link HumanAccountData} representing an account from a human. This is likely
 * to be extended once authentication is fleshed out.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface HumanAccountData extends AccountData {
  /**
   * Get the user's password digest. The digest can be used to authenticate
   * the user.
   * 
   * This method will return null if password based authentication is disabled
   * for the user (or if no password is set).
   * 
   * @return The user's password digest.
   */
  public PasswordDigest getPasswordDigest();
  
  /**
   * Reset the user's password to the specified string.
   * 
   * After this method is called, the caller must zero the password bytes to
   * ensure that the user's credentials don't leak.
   * 
   * @param password The user's new password
   */
  public void setPassword(char[] password);
}
