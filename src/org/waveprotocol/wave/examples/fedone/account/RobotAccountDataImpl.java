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

import com.google.common.base.Preconditions;

/**
 * Robot Account implementation.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class RobotAccountDataImpl implements RobotAccountData {

  private final String username;
  private final String url;
  private final String capabilitiesHash;
  private final boolean isVerified;

  /**
   * Creates a new {@link RobotAccountData}.
   *
   * @param username non-null username for this account.
   * @param url non-null Url where the robot can be reached.
   * @param capabilitiesHash the hash of the robot, may be null if not
   *        retrieved.
   * @param isVerified boolean indicating wether this {@link RobotAccountData}
   *        has been verified.
   */
  public RobotAccountDataImpl(
      String username, String url, String capabilitiesHash, boolean isVerified) {
    Preconditions.checkNotNull(username, "Username can not be null");
    Preconditions.checkNotNull(url, "Url can not be null");

    this.username = username;
    this.url = url;
    this.capabilitiesHash = capabilitiesHash;
    this.isVerified = isVerified;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isHuman() {
    return false;
  }

  @Override
  public HumanAccountData asHuman() {
    throw new UnsupportedOperationException("Can't turn a RobotAccount into a HumanAccount");
  }

  @Override
  public boolean isRobot() {
    return true;
  }

  @Override
  public RobotAccountData asRobot() {
    return this;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getCapabilitiesHash() {
    return capabilitiesHash;
  }

  @Override
  public boolean isVerified() {
    return isVerified;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((capabilitiesHash == null) ? 0 : capabilitiesHash.hashCode());
    result = prime * result + (isVerified ? 1231 : 1237);
    result = prime * result + url.hashCode();
    result = prime * result + username.hashCode();
    return result;
  }

  /**
   * Robots are equal if their username, url, capabilitiesHash and verification
   * are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof RobotAccountDataImpl)) {
      return false;
    }
    RobotAccountDataImpl other = (RobotAccountDataImpl) obj;

    return username.equals(other.username) && url.equals(other.url)
        && capabilitiesHash.equals(other.capabilitiesHash) && isVerified == other.isVerified;
  }
}