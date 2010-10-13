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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.DeltaSequence;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveClientRpc;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Implements the client front-end.
 *
 * This class maintains a list of wavelets accessible by local participants by
 * inspecting all updates it receives (there is no need to inspect historic
 * deltas as they would have been received as updates had there been an
 * addParticipant). Updates are aggregated in a special index Wave which is
 * stored with the WaveServer.
 *
 * When a wavelet is added and it's not at version 0, buffer updates until a
 * request for the wavelet's history has completed.
 */
public class ClientFrontendImpl implements ClientFrontend, WaveBus.Subscriber {
  private static final Log LOG = Log.get(ClientFrontendImpl.class);

  private final static AtomicInteger channel_counter = new AtomicInteger(0);

  /** Information we hold in memory for each wavelet, including index wavelets. */
  private static class PerWavelet {
    private final Set<ParticipantId> participants;
    private final AtomicLong timestamp;  // last modified time
    private final HashedVersion version0;
    private HashedVersion currentVersion;
    private String digest;

    PerWavelet(WaveletName waveletName, HashedVersion hashedVersionZero) {
      this.participants = Collections.synchronizedSet(Sets.<ParticipantId>newHashSet());
      this.timestamp = new AtomicLong(0);
      this.version0 = hashedVersionZero;
      this.currentVersion = version0;
      this.digest = "";
    }

    synchronized HashedVersion getCurrentVersion() {
      return currentVersion;
    }

    synchronized void setCurrentVersion(HashedVersion version) {
      this.currentVersion = version;
    }
  }

  @VisibleForTesting final Map<ParticipantId, UserManager> perUser;
  private final Map<WaveId, Map< WaveletId, PerWavelet>> perWavelet;
  private final WaveletProvider waveletProvider;
  private final HashedVersionFactory hashedVersionFactory;

  @Inject
  public ClientFrontendImpl(final HashedVersionFactory hashedVersionFactory,
      WaveletProvider waveletProvider, WaveBus wavebus) {
    this.waveletProvider = waveletProvider;
    this.hashedVersionFactory = hashedVersionFactory;

    final MapMaker mapMaker = new MapMaker();
    perWavelet = mapMaker.makeComputingMap(new Function<WaveId, Map<WaveletId, PerWavelet>>() {
      @Override
      public Map<WaveletId, PerWavelet> apply(final WaveId waveId) {
        return mapMaker.makeComputingMap(new Function<WaveletId, PerWavelet>() {
          @Override
          public PerWavelet apply(WaveletId waveletId) {
            WaveletName waveletName = WaveletName.of(waveId, waveletId);
            return new PerWavelet(waveletName, hashedVersionFactory.createVersionZero(waveletName));
          }
        });
      }
    });


    perUser = mapMaker.makeComputingMap(new Function<ParticipantId, UserManager>() {
      @Override
      public UserManager apply(ParticipantId from) {
        return new UserManager();
      }
    });

    // TODO(anorth): Fix unsafe publishing in constructor.
    wavebus.subscribe(this);
  }

  @Override
  public void openRequest(ParticipantId participant, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    LOG.info("received openRequest from " + participant + " for " + waveId + ", filter "
        + waveletIdFilter + ", known wavelets: " + knownWavelets);
    if (!knownWavelets.isEmpty()) {
      openListener.onFailure("Known wavelets not supported");
      return;
    }

    String channel_id = generateChannelID();
    boolean isIndexWave = IndexWave.isIndexWave(waveId);
    UserManager userManager = perUser.get(participant);
    synchronized (userManager) {
      WaveViewSubscription subscription =
          userManager.subscribe(waveId, waveletIdFilter, channel_id, openListener);

      Set<WaveletId> waveletIds = visibleWaveletsFor(subscription);
      for (WaveletId waveletId : waveletIds) {
        WaveletName waveletName = WaveletName.of(waveId, waveletId);
        // The WaveletName by which the waveletProvider knows the relevant deltas

        // TODO(anorth): if the client provides known wavelets, calculate
        // where to start sending deltas from.

        DeltaSequence deltasToSend;
        WaveletSnapshotAndVersion snapshotToSend;
        HashedVersion endVersion;

        if (isIndexWave) {
          // Fetch deltas from the real wave from which the index wavelet
          // is generated.
          // TODO(anorth): send a snapshot instead.
          WaveletName sourceWaveletName = IndexWave.waveletNameFromIndexWavelet(waveletName);

          endVersion = getWavelet(sourceWaveletName).currentVersion;
          HashedVersion startVersion = getWavelet(sourceWaveletName).version0;
          // Send deltas to bring the wavelet up to date
          DeltaSequence sourceWaveletDeltas = new DeltaSequence(
              waveletProvider.getHistory(sourceWaveletName, startVersion, endVersion), endVersion);
          // Construct fake index wave deltas from the deltas
          String newDigest = getWavelet(sourceWaveletName).digest;
          deltasToSend = IndexWave.createIndexDeltas(startVersion.getVersion(), sourceWaveletDeltas, "",
              newDigest);
          endVersion = deltasToSend.getEndVersion();
          snapshotToSend = null;
        } else {
          // Send a snapshot of the current state.
          // TODO(anorth): calculate resync point if the client already knows
          // a snapshot.
          deltasToSend = DeltaSequence.empty(HashedVersion.UNSIGNED_VERSION_0);
          snapshotToSend = waveletProvider.getSnapshot(waveletName);
          endVersion = CoreWaveletOperationSerializer.deserialize(
              snapshotToSend.snapshot.getVersion());
        }

        LOG.info("snapshot in response is: " + (snapshotToSend == null));
        if (snapshotToSend == null) {
          // Send deltas.
          openListener.onUpdate(waveletName, snapshotToSend, deltasToSend, endVersion,
              null, false, channel_id);
        } else {
          // Send the snapshot.
          openListener.onUpdate(waveletName, snapshotToSend, deltasToSend, endVersion,
              CoreWaveletOperationSerializer.deserialize(snapshotToSend.committedVersion), false,
              channel_id);
        }
      }

      WaveletName dummyWaveletName = createDummyWaveletName(waveId);
      if (waveletIds.size() == 0) {
        // Send message with just the channel id.
        LOG.info("sending just a channel id for " + dummyWaveletName);
        openListener.onUpdate(dummyWaveletName,
            null, new ArrayList<CoreWaveletDelta>(), null, null, false, channel_id);
      }

      LOG.info("sending marker for " + dummyWaveletName);
      openListener.onUpdate(dummyWaveletName,
          null, new ArrayList<CoreWaveletDelta>(), null, null, true, null);
    }
  }

  private String generateChannelID() {
    return "ch" + channel_counter.addAndGet(1);
  }

  private boolean isWaveletWritable(WaveletName waveletName) {
    return !IndexWave.isIndexWave(waveletName.waveId);
  }

  @Override
  public void submitRequest(final WaveletName waveletName, final ProtocolWaveletDelta delta,
      final String channelId, final SubmitRequestListener listener) {
    final ParticipantId author = new ParticipantId(delta.getAuthor());
    if (!isWaveletWritable(waveletName)) {
      listener.onFailure("Wavelet " + waveletName + " is readonly");
    } else {
      perUser.get(author).submitRequest(channelId, waveletName);
      waveletProvider.submitRequest(waveletName, delta, new SubmitRequestListener() {
        @Override
        public void onSuccess(int operationsApplied,
            HashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
          listener.onSuccess(operationsApplied, hashedVersionAfterApplication,
              applicationTimestamp);
          getWavelet(waveletName).timestamp.set(applicationTimestamp);
          perUser.get(author).submitResponse(channelId, waveletName,
              hashedVersionAfterApplication);
        }

        @Override
        public void onFailure(String error) {
          listener.onFailure(error);
          perUser.get(author).submitResponse(channelId, waveletName, null);
        }
      });
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    for (ParticipantId participant : getWavelet(waveletName).participants) {
      // TODO(arb): commits? channelId
      perUser.get(participant).onCommit(waveletName, version, null);
    }
  }

  private void participantAddedToWavelet(WaveletName waveletName, ParticipantId participant,
      HashedVersion version) {
    getWavelet(waveletName).participants.add(participant);
  }

  private void participantRemovedFromWavelet(WaveletName waveletName, ParticipantId participant) {
    getWavelet(waveletName).participants.remove(participant);
  }

  /**
   * Sends new deltas to a particular user on a particular wavelet, and also
   * generates fake deltas for the index wavelet. Updates the participants of
   * the specified wavelet if the participant was added or removed.
   *
   * @param waveletName which the deltas belong to
   * @param participant on the wavelet
   * @param newDeltas newly arrived deltas of relevance for participant. Must
   *        not be empty.
   * @param add whether the participant is added by the first delta
   * @param remove whether the participant is removed by the last delta
   * @param oldDigest The digest text of the wavelet before the deltas are
   *        applied (but including all changes from preceding deltas)
   * @param newDigest The digest text of the wavelet after the deltas are
   *        applied
   */
  @VisibleForTesting
  void participantUpdate(WaveletName waveletName, ParticipantId participant,
      DeltaSequence newDeltas, boolean add, boolean remove, String oldDigest, String newDigest) {
    if (add) {
      participantAddedToWavelet(waveletName, participant, newDeltas.getStartVersion());
    }
    perUser.get(participant).onUpdate(waveletName, newDeltas);
    if (remove) {
      participantRemovedFromWavelet(waveletName, participant);
    }

    // Construct and publish fake index wave deltas
    if (IndexWave.canBeIndexed(waveletName)) {
      WaveletName indexWaveletName = IndexWave.indexWaveletNameFor(waveletName.waveId);
      long startVersion = newDeltas.getStartVersion().getVersion();
      if (add) {
        HashedVersion v0 = hashedVersionFactory.createVersionZero(waveletName);
        participantAddedToWavelet(indexWaveletName, participant, v0);
        startVersion = 0;
      }
      DeltaSequence indexDeltas =
          IndexWave.createIndexDeltas(startVersion, newDeltas, oldDigest, newDigest);
      if (!indexDeltas.isEmpty()) {
        perUser.get(participant).onUpdate(indexWaveletName, indexDeltas);
      }
      if (remove) {
        participantRemovedFromWavelet(indexWaveletName, participant);
      }
    }
  }

  /**
   * Based on deltas we receive from the wave server, pass the appropriate
   * membership changes and deltas from both the affected wavelets and the
   * corresponding index wave wavelets on to the UserManagers.
   */
  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, HashedVersion endVersion,
      List<CoreWaveletDelta> newDeltas) {
    if (newDeltas.isEmpty()) {
      return;
    }

    WaveletName waveletName = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());
    PerWavelet waveletInfo = getWavelet(waveletName);
    HashedVersion expectedVersion;
    String oldDigest;
    Set<ParticipantId> remainingParticipants;

    synchronized (waveletInfo) {
      expectedVersion = waveletInfo.getCurrentVersion();
      oldDigest = waveletInfo.digest;
      remainingParticipants = Sets.newHashSet(waveletInfo.participants);
    }

    DeltaSequence deltaSequence = new DeltaSequence(newDeltas, endVersion);
    Preconditions.checkState(expectedVersion.equals(deltaSequence.getStartVersion()),
        "Expected deltas starting at version %s, got %s",
        expectedVersion, deltaSequence.getStartVersion().getVersion());
    String newDigest = digest(Snippets.renderSnippet(wavelet, 80));

    synchronized (waveletInfo) {
      waveletInfo.setCurrentVersion(deltaSequence.getEndVersion());
      waveletInfo.digest = newDigest;
    }

    // Participants added during the course of newDeltas
    Set<ParticipantId> newParticipants = Sets.newHashSet();

    for (int i = 0; i < newDeltas.size(); i++) {
      CoreWaveletDelta delta = newDeltas.get(i);
      // Participants added or removed in this delta get the whole delta
      for (CoreWaveletOperation op : delta.getOperations()) {
        if (op instanceof CoreAddParticipant) {
          ParticipantId p = ((CoreAddParticipant) op).getParticipantId();
          remainingParticipants.add(p);
          newParticipants.add(p);
        }
        if (op instanceof CoreRemoveParticipant) {
          ParticipantId p = ((CoreRemoveParticipant) op).getParticipantId();
          remainingParticipants.remove(p);
          participantUpdate(waveletName, p,
              deltaSequence.subList(0, i + 1), newParticipants.remove(p), true, oldDigest, "");
        }
      }
    }

    // Send out deltas to those who end up being participants at the end
    // (either because they already were, or because they were added).
    for (ParticipantId p : remainingParticipants) {
      boolean isNew = newParticipants.contains(p);
      participantUpdate(waveletName, p, deltaSequence, isNew, false, oldDigest, newDigest);
    }
  }

  private PerWavelet getWavelet(WaveletName name) {
    return perWavelet.get(name.waveId).get(name.waveletId);
  }

  private Set<WaveletId> visibleWaveletsFor(WaveViewSubscription subscription) {
    Set<WaveletId> visible = Sets.newHashSet();
    for (WaveletId w : perWavelet.get(subscription.getWaveId()).keySet()) {
      if (subscription.includes(w)) {
        visible.add(w);
      }
    }
    return visible;
  }

  /** Constructs a digest of the specified String. */
  private static String digest(String text) {
    int digestEndPos = text.indexOf('\n');
    if (digestEndPos < 0) {
      return text;
    } else {
      return text.substring(0, Math.min(80, digestEndPos));
    }
  }

  @VisibleForTesting
  static DeltaSequence createUnsignedDeltas(List<CoreWaveletDelta> deltas) {
    Preconditions.checkArgument(!deltas.isEmpty(), "No deltas specified");
    CoreWaveletDelta lastDelta = Iterables.getLast(deltas);
    long endVersion = lastDelta.getTargetVersion().getVersion() + lastDelta.getOperations().size();
    return new DeltaSequence(deltas, HashedVersion.unsigned(endVersion));
  }

  @VisibleForTesting
  static WaveletName createDummyWaveletName(WaveId waveId) {
    final WaveletName dummyWaveletName =
      WaveletName.of(waveId, new WaveletId(waveId.getDomain(), "dummy+root"));
    return dummyWaveletName;
  }
}