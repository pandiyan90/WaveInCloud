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

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.Collection;

/**
 * Provides wavelet snapshots and history, and accepts delta submissions to
 * wavelets.
 */
public interface WaveletProvider {

  /**
   * Receives the result of a delta submission request.
   */
  interface SubmitRequestListener {
    /**
     * Notifies the listener that the delta was successfully applied.
     *
     * @param operationsApplied number of operations applied
     * @param hashedVersionAfterApplication wavelet hashed version after the delta
     * @param applicationTimestamp timestamp of delta application
     */
    void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
        long applicationTimestamp);

    /**
     * Notifies the listener that the delta failed to apply.
     */
    void onFailure(String errorMessage);
  }

  /**
   * Request that a given delta is submitted to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param delta to be submitted to the server.
   * @param listener callback which will return the result of the submission.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      SubmitRequestListener listener);

  /**
   * Retrieve the wavelet history of deltas applied to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param versionStart start version (inclusive), minimum 0.
   * @param versionEnd end version (exclusive).
   * @return deltas in the range as requested, ordered by applied version,
   *         or null if there was an error. If a delta straddles
   *         one of the requested version boundaries, it will be included.
   */
  Collection<VersionedWaveletDelta> getHistory(WaveletName waveletName,
      HashedVersion versionStart, HashedVersion versionEnd);

  /**
   * Request the current state of the wavelet.
   *
   * @param waveletName the name of the wavelet
   * @return the wavelet, or null if the wavelet doesn't exist
   */
  WaveletSnapshotAndVersion getSnapshot(WaveletName waveletName);
}
