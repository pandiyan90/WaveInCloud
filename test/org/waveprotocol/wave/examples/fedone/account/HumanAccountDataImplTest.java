/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.account;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class HumanAccountDataImplTest extends TestCase {
  public void testUserAddressReturnsCorrectResult() {
    ParticipantId id = ParticipantId.ofUnsafe("drhorrible@example.com");
    HumanAccountData account = new HumanAccountDataImpl(id);
    assertEquals(account.getId(), id);
    assertTrue(account.isHuman());
    assertFalse(account.isRobot());
  }

  public void testPasswordDigestVerifies() {
    HumanAccountData account = new HumanAccountDataImpl(
        ParticipantId.ofUnsafe("captainhammer@example.com"), "wonderflownium".toCharArray());

    assertNotNull(account.getPasswordDigest());
    assertTrue(account.getPasswordDigest().verify("wonderflownium".toCharArray()));
  }

  public void testUserWithNoPasswordHasNoPasswordDigest() {
    HumanAccountData account =
        new HumanAccountDataImpl(ParticipantId.ofUnsafe("moist@example.com"));

    assertNull(account.getPasswordDigest());
  }
}
