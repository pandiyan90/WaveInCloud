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

package org.waveprotocol.box.server.frontend;

import com.google.inject.internal.Nullable;

import org.waveprotocol.box.server.waveserver.WaveClientRpc;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collection;
import java.util.List;


/**
 * The client front-end handles requests from clients and directs them to
 * appropriate back-ends.
 *
 * Provides updates for wavelets that a client has opened and access to.
 */
public interface ClientFrontend {

  /**
   * Listener provided to open requests.
   */
  interface OpenListener {
    /**
     * Called when an update is received.
     */
    void onUpdate(WaveletName waveletName, @Nullable WaveletSnapshotAndVersion snapshot,
        List<CoreWaveletDelta> deltas, @Nullable HashedVersion endVersion,
        @Nullable HashedVersion committedVersion, boolean hasMarker, String channel_id);

    /**
     * Called when the stream fails. No further updates will be received.
     */
    void onFailure(String errorMessage);
  }

  /**
   * Request submission of a delta.
   *
   * @param waveletName name of wavelet.
   * @param delta the wavelet delta to submit.
   * @param channelId the client's channel ID
   * @param listener callback for the result.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta, String channelId,
      WaveletProvider.SubmitRequestListener listener);

  /**
   * Request to open a Wave. Optional waveletIdPrefixes allows the requester to
   * constrain which wavelets to include in the updates.
   *
   * @param participant which is doing the requesting.
   * @param waveId the wave id.
   * @param waveletIdFilter filter over wavelets to open
   * @param knownWavelets a collection of wavelet versions the client already
   *        knows
   * @param openListener callback for updates.
   */
  void openRequest(ParticipantId participant, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener);
}