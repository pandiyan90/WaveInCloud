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

package org.waveprotocol.box.server.persistence.memory;

import com.google.gxp.com.google.common.base.Preconditions;

import org.waveprotocol.box.server.waveserver.AppliedDeltaUtil;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * An in-memory implementation of DeltasAccess
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class MemoryDeltaCollection implements DeltasAccess {
  private final Map<Long, WaveletDeltaRecord> deltas = CollectionUtils.newHashMap();
  private final WaveletName waveletName;

  private HashedVersion endVersion = null;

  public MemoryDeltaCollection(WaveletName waveletName) {
    Preconditions.checkNotNull(waveletName);
    this.waveletName = waveletName;
  }

  @Override
  public boolean isEmpty() {
    return deltas.isEmpty();
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public HashedVersion getEndVersion() {
    return endVersion;
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) {
    return deltas.get(version);
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) throws IOException {
    WaveletDeltaRecord record = getDelta(version);

    if (record == null) {
      return null;
    }

    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta = record.applied;
    if (appliedDelta == null) {
      return null;
    }

    return AppliedDeltaUtil.getHashedVersionAppliedAt(appliedDelta);
  }

  @Override
  public HashedVersion getResultingVersion(long version) {
    WaveletDeltaRecord delta = getDelta(version);

    if (delta != null) {
      return delta.transformed.getResultingVersion();
    } else {
      return null;
    }
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version) {
    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.applied : null;
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) {
    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.transformed : null;
  }

  @Override
  public void close() {
    // Does nothing.
  }

  @Override
  public void append(Collection<WaveletDeltaRecord> newDeltas) {
    for (WaveletDeltaRecord delta : newDeltas) {
      deltas.put(delta.transformed.getAppliedAtVersion(), delta);
      endVersion = delta.transformed.getResultingVersion();
    }
  }
}