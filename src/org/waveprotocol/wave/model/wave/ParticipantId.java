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

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * A ParticipantId uniquely identifies a participant. It looks like an email
 * address, e.g. 'joe@gmail.com'
 */
public final class ParticipantId {

  /** The prefix of a domain in the ParticpantId */
  public static final String DOMAIN_PREFIX = "@";

  /** The participant's address */
  private final String address;

  /**
   * Constructs an id.
   * 
   * @param address a non-null address string
   * @deprecated Use the factory methods instead
   */
  @Deprecated
  public ParticipantId(String address) {
    Preconditions.checkNotNull(address, "Non-null address expected");

    address = normalize(address);
    this.address = address;
  }

  /**
   * Normalizes an address.
   * 
   * @param address address to normalize; may be null
   * @return normal form of {@code address} if non-null; null otherwise.
   */
  private static String normalize(String address) {
    if (address == null) {
      return null;
    }
    return address.toLowerCase();
  }

  /**
   * Validates the given address. Validation currently only checks whether one
   * and only one @ symbol is present.
   * 
   * @param address the address to validate
   * @throws InvalidParticipantAddress if the validation fails.
   */
  private static void validate(String address) throws InvalidParticipantAddress {
    if (!address.matches("^.*" + DOMAIN_PREFIX + ".+$")) {
      // TODO: Check the validity of the username and domain part
      throw new InvalidParticipantAddress(address, "Invalid address specified");
    }
  }

  /**
   * @return the participant's address
   */
  public String getAddress() {
    return address;
  }

  /**
   * @return the domain name in the address. If no "@" occurs, it will be the
   *         whole string, if more than one occurs, it will be the part after
   *         the last "@".
   */
  public String getDomain() {
    String[] parts = address.split("@");
    return parts[parts.length - 1];
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof ParticipantId) {
      ParticipantId p = (ParticipantId) o;
      return address.equals(p.address);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public String toString() {
    return getAddress();
  }

  /**
   * Constructs a {@link ParticipantId} with the supplied address. The given
   * address will be validated.
   * 
   * @param address the address to construct a {@link ParticipantId} for
   * @return an instance of {@link ParticipantId} constructed using the given
   *         address.
   * @throws InvalidParticipantAddress if the validation on the address fails.
   */
  public static ParticipantId of(String address) throws InvalidParticipantAddress {
    validate(address);
    return new ParticipantId(address);
  }

  /**
   * Constructs a {@link ParticipantId} with the given address. It will validate
   * the given address. It is unsafe because it will throw an unchecked
   * exception if the validation fails.
   * 
   * @param address the address of the Participant
   * @return an instance of {@link ParticipantId} constructed using the given
   *         address.
   * @throws IllegalArgumentException if the validation on the address fails
   */
  public static ParticipantId ofUnsafe(String address) {
    try {
      return of(address);
    } catch (InvalidParticipantAddress e) {
      throw new IllegalArgumentException(e);
    }
  }

}
