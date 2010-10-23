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

package org.waveprotocol.box.server.rpc;

import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.util.TestDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;

/**
 * Stub of {@link WaveletProvider} for testing. It only supports getSnapshot().
 *
 * It currently hosts a single wavelet, which contains a single document.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveletProviderStub implements WaveletProvider {
  private final WaveletData wavelet;
  private HashedVersion currentVersionOverride;
  private ProtocolHashedVersion committedVersion;
  private boolean allowsAccess = true;

  public WaveletProviderStub() {
    wavelet = TestDataUtil.createSimpleWaveletData();

    // This will be null in practice until the persistence store is in place.
    setCommittedVersion(null);
  }

  @Override
  public WaveletSnapshotAndVersion getSnapshot(WaveletName waveletName) {
    final byte[] JUNK_BYTES = new byte[] {0, 1, 2, 3, 4, 5, -128, 127};

    if (waveletName.waveId.equals(getHostedWavelet().getWaveId())
        && waveletName.waveletId.equals(getHostedWavelet().getWaveletId())) {
      HashedVersion version =
          (currentVersionOverride != null) ? currentVersionOverride : HashedVersion.of(
              getHostedWavelet().getVersion(), JUNK_BYTES);
      return new WaveletSnapshotAndVersion(
          SnapshotSerializer.serializeWavelet(getHostedWavelet(), version), getCommittedVersion());
    } else {
      return null;
    }
  }

  @Override
  public Collection<TransformedWaveletDelta> getHistory(WaveletName waveletName,
      HashedVersion versionStart, HashedVersion versionEnd) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void submitRequest(
      WaveletName waveletName, ProtocolWaveletDelta delta, SubmitRequestListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean checkAccessPermission(WaveletName waveletName, ParticipantId participantId) {
    return allowsAccess;
  }

  /**
   * @return the wavelet
   */
  public WaveletData getHostedWavelet() {
    return wavelet;
  }

  /**
   * @param currentVersionOverride the currentVersionOverride to set
   */
  public void setVersionOverride(HashedVersion currentVersionOverride) {
    this.currentVersionOverride = currentVersionOverride;
  }

  /**
   * @param committedVersion the committedVersion to set
   */
  public void setCommittedVersion(ProtocolHashedVersion committedVersion) {
    this.committedVersion = committedVersion;
  }

  /**
   * @return the committedVersion
   */
  public ProtocolHashedVersion getCommittedVersion() {
    return committedVersion;
  }

  /**
   * @param allowsAccess whether or not users have access permissions
   */
  public void setAllowsAccess(boolean allowsAccess) {
    this.allowsAccess = allowsAccess;
  }
}
