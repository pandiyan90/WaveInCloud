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

package org.waveprotocol.box.server.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuffer;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.MuteDocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for {@link WaveletData}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class WaveletDataUtil {

  // TODO(ljvderijk): Schemas should be enforced, see issue 109.
  private static final ObservableWaveletData.Factory<?> WAVELET_FACTORY =
      WaveletDataImpl.Factory.create(new MuteDocumentFactory(SchemaCollection.empty()));

  private WaveletDataUtil() {
  }

  /**
   * Returns the {@link WaveletName} for the given wavelet.
   *
   * @param wavelet the wavelet to get the name for
   */
  public static WaveletName waveletNameOf(ReadableWaveletData wavelet) {
    return WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());
  }

  /**
   * Apply a delta to the given wavelet. Rolls back the operation if it fails.
   *
   * @param delta delta to apply.
   * @param wavelet the wavelet to apply the operations to.
   *
   * @throws OperationException if the operations fail to apply (and are
   *         successfully rolled back).
   * @throws IllegalStateException if the operations have failed and can not be
   *         rolled back.
   */
  public static void applyWaveletDelta(TransformedWaveletDelta delta, WaveletData wavelet)
      throws OperationException {
    Preconditions.checkState(wavelet != null, "wavelet may not be null");
    Preconditions.checkState(delta.getAppliedAtVersion() == wavelet.getVersion(),
        "Delta's version %s doesn't apply to wavelet at %s", delta.getAppliedAtVersion(),
        wavelet.getVersion());

    List<WaveletOperation> reverseOps = new ArrayList<WaveletOperation>();
    WaveletOperation lastOp = null;
    int opsApplied = 0;
    try {
      for (WaveletOperation op : delta.getOperations()) {
        lastOp = op;
        List<? extends WaveletOperation> reverseOp = op.applyAndReturnReverse(wavelet);
        reverseOps.addAll(reverseOp);
        opsApplied++;
      }
    } catch (OperationException e) {
      // Deltas are atomic, so roll back all operations that were successful
      rollbackWaveletOperations(wavelet, reverseOps);
      throw new OperationException("Only applied " + opsApplied + " of "
          + delta.getOperations().size() + " operations at version " + wavelet.getVersion()
          + ", rolling back, failed op was " + lastOp, e);
    }
  }

  /**
   * Like applyWaveletOperations, but throws an {@link IllegalStateException}
   * when ops fail to apply. Is used for rolling back operations.
   *
   * @param ops to apply for rollback
   */
  private static void rollbackWaveletOperations(WaveletData wavelet, List<WaveletOperation> ops) {
    for (int i = ops.size() - 1; i >= 0; i--) {
      try {
        ops.get(i).apply(wavelet);
      } catch (OperationException e) {
        throw new IllegalStateException(
            "Failed to roll back " + ops.get(i) + " with inverse " + ops.get(i), e);
      }
    }
  }

  /**
   * Creates an empty wavelet.
   *
   * @param waveletName the name of the wavelet.
   * @param author the author of the wavelet.
   * @param creationTimestamp the time at which the wavelet is created.
   */
  public static ObservableWaveletData createEmptyWavelet(WaveletName waveletName,
      ParticipantId author, HashedVersion version, long creationTimestamp) {
    return copyWavelet(new EmptyWaveletSnapshot(waveletName.waveId, waveletName.waveletId, author,
        version, creationTimestamp));
  }

  /**
   * Copies a wavelet.
   *
   * @param wavelet the wavelet to copy.
   * @return A mutable copy.
   */
  public static ObservableWaveletData copyWavelet(ReadableWaveletData wavelet) {
    return WAVELET_FACTORY.create(wavelet);
  }

  /**
   * Adds an empty blip to the given wavelet.
   *
   * @param wavelet the wavelet to add the blip to.
   * @param blipId the id of the blip to add.
   * @param author the author of this blip (will also be the only participant).
   * @param time the time to set in the blip as creation/lastmodified time.
   */
  public static BlipData addEmptyBlip(
      WaveletData wavelet, String blipId, ParticipantId author, long time) {
    return wavelet.createBlip(blipId,
        author,
        Lists.newArrayList(author),
        new DocInitializationBuffer().finish(),
        time,
        time);
  }


  /**
   * Reads all deltas from the given iterator and constructs the end
   * wavelet state by successive application of all deltas beginning
   * from the empty wavelet.
   *
   * @param waveletName the name of the wavelet.
   * @param deltas non-empty, contiguous sequence of non-empty deltas beginning
   *        from version zero.
   */
  public static ObservableWaveletData buildWaveletFromDeltas(WaveletName waveletName,
      Iterator<TransformedWaveletDelta> deltas) throws OperationException {
    Preconditions.checkArgument(deltas.hasNext(), "empty deltas");

    // Read the first delta now to get creator and creation time.
    TransformedWaveletDelta delta = deltas.next();
    Preconditions.checkArgument(delta.getAppliedAtVersion() == 0,
        "first delta has non-zero version: " + delta.getAppliedAtVersion());
    ParticipantId creator = delta.getAuthor();
    long creationTimestamp = delta.getApplicationTimestamp();

    // Bootstrap the wavelet state to apply deltas to in the loop below.
    ObservableWaveletData wavelet = createEmptyWavelet(
        // garbage hashed version doesn't matter as it's overwritten by first delta below
        waveletName, creator, HashedVersion.unsigned(0), creationTimestamp);

    // Apply
    for (;;) {
      applyWaveletDelta(delta, wavelet);
      Preconditions.checkState(wavelet.getHashedVersion().getVersion() == wavelet.getVersion());
      Preconditions.checkState(wavelet.getHashedVersion().equals(delta.getResultingVersion()));
      if (deltas.hasNext()) {
        delta = deltas.next();
      } else {
        break;
      }
    }
    return wavelet;
  }
}
