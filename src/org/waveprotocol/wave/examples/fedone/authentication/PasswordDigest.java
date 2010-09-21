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

package org.waveprotocol.wave.examples.fedone.authentication;


import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A simple, secure password class.
 * 
 * Passwords are stored using a salted SHA-384 digest. To persist a password
 * object, use Java's serialization interface or save the salt and digest,
 * and recreate the password object using Password.from(salt, digest).
 *
 * Character arrays are used instead of strings so the contents can be cleared
 * before they are garbage collected. (Java's strings are immutable). Passwords
 * should never be stored as strings at any intermediate stage.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class PasswordDigest implements Serializable {
  public static final int DEFAULT_SALT_LENGTH = 16;
  
  private final byte[] salt;
  private byte[] digest;

  // The random number generator is reused, as allocating the RNG each time we
  // need one requires entropy, and is thus quite expensive.
  private static ThreadLocal<SecureRandom> rng = new ThreadLocal<SecureRandom>() {
    @Override
    protected SecureRandom initialValue() {
      return new SecureRandom();
    }
  };
  
  /**
   * Create a new password object store. Use set() to set the password contents.
   */
  public PasswordDigest() {
    salt = generateSalt();
  }
  
  /**
   * Helper for deserializing passwords. Use from().
   */
  private PasswordDigest(byte[] salt, byte[] digest) {
    Preconditions.checkNotNull(salt);
    Preconditions.checkNotNull(digest);
    // A short salt makes passwords susceptible to rainbow tables.
    Preconditions.checkArgument(salt.length >= 10);
    // We don't need to check the digest length - if the digest is invalid,
    // verify() will always fail.
    this.salt = salt;
    this.digest = digest;
  }
  
  private static byte[] createPasswordDigest(char[] password, byte[] salt) {
    MessageDigest hash;
    try {
      hash = MessageDigest.getInstance("SHA-384");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Your java environment doesn't support "
          + "the expected cryptographic hash functions", e);
    }
    
    hash.update(salt);
    ByteBuffer passwordBytes = Charset.forName("UTF-8").encode(CharBuffer.wrap(password));
    hash.update(passwordBytes);
    return hash.digest();
  }
  
  private static byte[] generateSalt(int length) {
    byte[] new_salt = new byte[length];
    rng.get().nextBytes(new_salt);
    return new_salt;
  }
  
  private static byte[] generateSalt() {
    return generateSalt(DEFAULT_SALT_LENGTH);
  }
  
  /**
   * Verify that the stored password matches the password provided.
   * 
   * @param providedPassword Password to compare to the password stored
   * @return true if the passwords match, false otherwise.
   */
  public boolean verify(char[] providedPassword) {
    return digest != null
        && Arrays.equals(digest, createPasswordDigest(providedPassword, salt)); 
  }
  
  /**
   * Set the value of the password to the specified string.
   * 
   * @param newPassword The new password value.
   */
  public void set(char[] newPassword) {
    digest = createPasswordDigest(newPassword, salt);
  }

  /**
   * Get the salt bytes.
   * 
   * @return A copy of the password's salt
   */
  public byte[] getSalt() {
    return salt.clone();
  }

  /**
   * Get the password digest.
   * 
   * @return A copy of the password's digest
   */
  public byte[] getDigest() {
    return digest.clone();
  }
  
  /**
   * Create a password from the specified salt and digest.
   */
  public static PasswordDigest from(byte[] salt, byte[] digest) {
    return new PasswordDigest(salt.clone(), digest.clone());
  }
}
