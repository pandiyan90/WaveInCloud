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

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.core.CoreTransform;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Contains the history of a wavelet - applied and transformed deltas plus the content
 * of the wavelet.
 */
abstract class WaveletContainerImpl implements WaveletContainer {

  private static final Log LOG = Log.get(WaveletContainerImpl.class);

  private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  protected static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  private final NavigableMap<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>>
      appliedDeltas = Maps.newTreeMap();
  private final NavigableMap<HashedVersion, CoreWaveletDelta> transformedDeltas =
      Maps.newTreeMap();
  private final Lock readLock;
  private final Lock writeLock;
  private WaveletData waveletData;
  protected WaveletName waveletName;
  protected HashedVersion currentVersion;
  protected HashedVersion lastCommittedVersion;
  protected State state;

  /**
   * Constructs an empty WaveletContainer for a wavelet with the given name.
   * waveletData is not set until a delta has been applied.
   *
   * @param waveletName the name of the wavelet.
   */
  public WaveletContainerImpl(WaveletName waveletName) {
    this.waveletName = waveletName;
    waveletData = null;
    currentVersion = HASH_FACTORY.createVersionZero(waveletName);
    lastCommittedVersion = null;

    // Configure the locks used by this Wavelet.
    final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    readLock = readWriteLock.readLock();
    writeLock = readWriteLock.writeLock();
    state = State.OK;
  }

  protected void acquireReadLock() {
    readLock.lock();
  }

  protected void releaseReadLock() {
    readLock.unlock();
  }

  protected void acquireWriteLock() {
    writeLock.lock();
  }

  protected void releaseWriteLock() {
    writeLock.unlock();
  }

  protected void assertStateOk() throws WaveletStateException {
    if (state != State.OK) {
      throw new WaveletStateException(state, "The wavelet is not in a usable state. ");
    }
  }

  @Override
  public State getState() {
    acquireReadLock();
    try {
      return state;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public void setState(State state) {
    acquireWriteLock();
    try {
      this.state = state;
    } finally {
      releaseWriteLock();
    }
  }

  @Override
  public boolean checkAccessPermission(ParticipantId participantId) throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // ParticipantId will be null if the user isn't logged in. A user who isn't logged in should
      // have access to public waves once they've been implemented.
      return participantId != null && waveletData.getParticipants().contains(participantId);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public HashedVersion getLastCommittedVersion() throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      return lastCommittedVersion;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public WaveletData getWaveletData() {
    return waveletData;
  }

  @Override
  public WaveletSnapshotAndVersion getSnapshot() {
    acquireWriteLock();
    try {
      // TODO: enable when we have persistence. Snapshots should only ever
      // be of committed versions as otherwise they may contain information
      // that could be lost for ever.
//      Preconditions.checkState(waveletData.getVersion() == lastCommittedVersion.getVersion(),
//          "Snapshot version doesn't match committed version");
      HashedVersion committedVersion = currentVersion;
      return new WaveletSnapshotAndVersion(
          SnapshotSerializer.serializeWavelet(waveletData, currentVersion),
          CoreWaveletOperationSerializer.serialize(committedVersion));
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Transform a wavelet delta if it has been submitted against a different head (currentVersion).
   * Must be called with write lock held.
   *
   * @param delta to possibly transform
   * @return the transformed delta and the version it was applied at
   *   (the version is the current version of the wavelet, unless the delta is
   *   a duplicate in which case it is the version at which it was originally
   *   applied)
   * @throws InvalidHashException if submitting against same version but different hash
   * @throws OperationException if transformation fails
   */
  protected CoreWaveletDelta maybeTransformSubmittedDelta(CoreWaveletDelta delta)
      throws InvalidHashException, OperationException {
    HashedVersion targetVersion = delta.getTargetVersion();
    if (targetVersion.equals(currentVersion)) {
      // Applied version is the same, we're submitting against head, don't need to do OT
      return delta;
    } else {
      // Not submitting against head, we need to do OT, but check the versions really are different
      if (targetVersion.getVersion() == currentVersion.getVersion()) {
        LOG.warning("Mismatched hash, expected " + currentVersion + ") but delta targets (" +
            targetVersion + ")");
        throw new InvalidHashException(currentVersion, targetVersion);
      } else {
        return transformSubmittedDelta(delta);
      }
    }
  }

  /**
   * Apply the operations from a single delta to the wavelet container.
   *
   * @param delta {@link CoreWaveletDelta} to apply, must be non-empty.
   * @param applicationTimeStamp timestamp of the application.
   */
  protected void applyWaveletOperations(CoreWaveletDelta delta, long applicationTimeStamp)
      throws OperationException {
    Preconditions.checkArgument(!delta.getOperations().isEmpty(), "empty delta");

    if (waveletData == null) {
      Preconditions.checkState(currentVersion.getVersion() == 0L, "CurrentVersion must be 0");
      waveletData = WaveletDataUtil.createEmptyWavelet(waveletName, delta.getAuthor(),
          currentVersion, applicationTimeStamp);
    }

    // TODO(anorth): Plumb a TransformedWaveletDelta with the right hashed version.
    HashedVersion endVersion = HashedVersion.unsigned(
        waveletData.getVersion() + delta.getOperations().size());
    WaveletDataUtil.applyWaveletDelta(delta, waveletData, endVersion, applicationTimeStamp);
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private CoreWaveletDelta transformSubmittedDelta(
      CoreWaveletDelta submittedDelta)
      throws OperationException, InvalidHashException {
    HashedVersion appliedVersion = submittedDelta.getTargetVersion();
    NavigableMap<HashedVersion, CoreWaveletDelta> serverDeltas =
        transformedDeltas.tailMap(transformedDeltas.floorKey(appliedVersion), true);

    if (serverDeltas.isEmpty()) {
      LOG.warning("Got empty server set, but not sumbitting to head! " + submittedDelta);
      // Not strictly an invalid hash, but it's a related issue
      throw new InvalidHashException(HashedVersion.unsigned(0), appliedVersion);
    }

    // Confirm that the target version/hash of this delta is valid.
    if (!serverDeltas.firstEntry().getKey().equals(appliedVersion)) {
      LOG.warning("Mismatched hashes: expected: " + serverDeltas.firstEntry().getKey() +
          " got: " + appliedVersion);
      throw new InvalidHashException(serverDeltas.firstEntry().getKey(), appliedVersion);
    }

    ParticipantId clientAuthor = submittedDelta.getAuthor();
    List<? extends CoreWaveletOperation> clientOps = submittedDelta.getOperations();
    for (Map.Entry<HashedVersion, CoreWaveletDelta> d : serverDeltas.entrySet()) {
      // If the client delta transforms to nothing before we've traversed all the server
      // deltas, return the version at which the delta was obliterated (rather than the
      // current version) to ensure that delta submission is idempotent.
      if (clientOps.isEmpty()) {
        return new CoreWaveletDelta(clientAuthor, d.getKey(), clientOps);
      }
      CoreWaveletDelta coreDelta = d.getValue();
      ParticipantId serverAuthor = coreDelta.getAuthor();
      List<? extends CoreWaveletOperation> serverOps = coreDelta.getOperations();
      if (clientAuthor.equals(serverAuthor) && clientOps.equals(serverOps)) {
        return coreDelta;
      }
      clientOps = transformOps(clientOps, clientAuthor, serverOps, serverAuthor);
    }
    return new CoreWaveletDelta(clientAuthor, currentVersion, clientOps);
  }

  /**
   * Transforms the specified client operations against the specified server operations,
   * returning the transformed client operations in a new list.
   *
   * @param clientOps may be unmodifiable
   * @param clientAuthor
   * @param serverOps may be unmodifiable
   * @param serverAuthor
   * @return The transformed client ops
   */
  private List<CoreWaveletOperation> transformOps(List<? extends CoreWaveletOperation> clientOps,
      ParticipantId clientAuthor, List<? extends CoreWaveletOperation> serverOps,
      ParticipantId serverAuthor) throws OperationException {
    List<CoreWaveletOperation> transformedClientOps = new ArrayList<CoreWaveletOperation>();

    for (CoreWaveletOperation c : clientOps) {
      for (CoreWaveletOperation s : serverOps) {
        OperationPair<CoreWaveletOperation> pair;
        try {

          pair = CoreTransform.transform(c, clientAuthor, s, serverAuthor);
        } catch (TransformException e) {
          throw new OperationException(e);
        }
        c = pair.clientOp();
      }
      transformedClientOps.add(c);
    }
    return transformedClientOps;
  }

  /**
   * Commit an applied delta to this wavelet container.
   *
   * @param appliedDelta to commit
   * @param transformedDelta of the applied delta
   * @return result of the application
   */
  protected DeltaApplicationResult commitAppliedDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta,
      CoreWaveletDelta transformedDelta) {
    int operationsApplied = appliedDelta.getMessage().getOperationsApplied();
    // Sanity check.
    Preconditions.checkState(currentVersion.equals(transformedDelta.getTargetVersion()));
    Preconditions.checkArgument(operationsApplied == transformedDelta.getOperations().size());

    transformedDeltas.put(currentVersion, transformedDelta);
    appliedDeltas.put(currentVersion, appliedDelta);

    HashedVersion versionAfterApplication = HASH_FACTORY.create(
        appliedDelta.getByteArray(), currentVersion, operationsApplied);
    currentVersion = versionAfterApplication;

    return new DeltaApplicationResult(appliedDelta, transformedDelta, versionAfterApplication);
  }

  /**
   * Returns the applied delta that was applied at a given hashed version.
   *
   * @param version the version to look up
   * @return the applied delta applied at the specified hashed version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDelta(
      HashedVersion version) {
    return appliedDeltas.get(version);
  }

  @Override
  public Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> requestHistory(
      HashedVersion versionStart, HashedVersion versionEnd)
      throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.

      Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> result =
          appliedDeltas.subMap(versionStart, versionEnd).values();
      LOG.info("### HR " + versionStart.getVersion() + " - " + versionEnd.getVersion() + ", " +
          result.size() + " deltas");
      return result;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public Collection<CoreWaveletDelta> requestTransformedHistory(HashedVersion versionStart,
      HashedVersion versionEnd) throws WaveletStateException {
    HashedVersion start = versionStart;
    HashedVersion end = versionEnd;
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.
      return transformedDeltas.subMap(transformedDeltas.floorKey(start), end).values();
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public Set<ParticipantId> getParticipants() {
    acquireReadLock();
    try {
      return (waveletData != null ? waveletData.getParticipants() : ImmutableSet.<
          ParticipantId>of());
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public HashedVersion getCurrentVersion() {
    return currentVersion;
  }
}
