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

package org.waveprotocol.box.server.waveserver;

import static org.mockito.Mockito.mock;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.waveserver.WaveletState.PersistenceListener;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link WaveletState} implementations.
 *
 * @author anorth@google.com (Alex North)
 */
public abstract class WaveletStateTestBase extends TestCase {

  private static final class DeltaFacets {
    final HashedVersion appliedAtVersion;
    final ByteStringMessage<ProtocolAppliedWaveletDelta> applied;
    final TransformedWaveletDelta transformed;

    DeltaFacets(HashedVersion appliedAtVersion, ByteStringMessage<ProtocolAppliedWaveletDelta> applied,
        TransformedWaveletDelta transformed) {
      this.appliedAtVersion = appliedAtVersion;
      this.applied = applied;
      this.transformed = transformed;
    }

    HashedVersion getResultingVersion() {
      return transformed.getResultingVersion();
    }
  }

  private static final WaveletName NAME = WaveletName.of(new WaveId("example.com", "waveid"),
      new WaveletId("example.com", "waveletid"));
  private static final ParticipantId AUTHOR = ParticipantId.ofUnsafe("author@example.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(AUTHOR);
  private static final long TS = 1234567890L;
  private static final long TS2 = TS + 1;
  private static final long TS3 = TS2 + 1;

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final HashedVersion V0 = HASH_FACTORY.createVersionZero(NAME);


  /**
   * Creates a new, empty wavelet state.
   */
  protected abstract WaveletState createEmptyState(WaveletName name);

  /**
   * Waits for all pending persistence operations to be completed. All
   * persistence listener callbacks must be completed before this method
   * returns.
   */
  protected abstract void awaitPersistence();

  private DeltaFacets d1;
  private DeltaFacets d2;
  private DeltaFacets d3;

  private WaveletState target;

  @Override
  public void setUp() throws Exception {
    d1 = makeDelta(V0, TS, 2);
    d2 = makeDelta(d1.getResultingVersion(), TS2, 2);
    d3 = makeDelta(d2.getResultingVersion(), TS3, 1);

    target = createEmptyState(NAME);
  }

  public void testReportsWaveletName() {
    assertEquals(NAME, target.getWaveletName());
  }

  public void testEmptyStateIsEmpty() {
    assertNull(target.getSnapshot());
    assertEquals(V0, target.getCurrentVersion());
    assertEquals(V0, target.getHashedVersion(0));

    assertNull(target.getTransformedDelta(V0));
    assertNull(target.getAppliedDelta(V0));
  }

  public void testSnapshotMetadataReflectsDeltas() throws Exception {
    HashedVersion v2 = d1.getResultingVersion();
    appendDeltas(d1);

    assertEquals(v2, target.getCurrentVersion());
    ReadableWaveletData snapshot = target.getSnapshot();
    assertEquals(AUTHOR, snapshot.getCreator());
    assertEquals(v2, snapshot.getHashedVersion());
    assertEquals(TS, snapshot.getCreationTime());
    assertEquals(TS, snapshot.getLastModifiedTime());
    assertEquals(2, snapshot.getVersion());

    HashedVersion v4 = d2.getResultingVersion();
    appendDeltas(d2);

    assertEquals(v4, target.getCurrentVersion());
    snapshot = target.getSnapshot();
    assertEquals(v4, snapshot.getHashedVersion());
    assertEquals(4, snapshot.getVersion());
    // Last-modified-time doesn't change due to unworthiness.
  }

  public void testHashedVersionAccessibleOnDeltaBoundaries() throws Exception {
    appendDeltas(d1, d2, d3);
    assertEquals(V0, target.getHashedVersion(0));
    assertEquals(d1.getResultingVersion(), target.getHashedVersion(2));
    assertEquals(d2.getResultingVersion(), target.getHashedVersion(4));
    assertEquals(d3.getResultingVersion(), target.getHashedVersion(5));
    assertNull(target.getHashedVersion(1));
    assertNull(target.getHashedVersion(3));
    assertNull(target.getHashedVersion(6));
  }

  public void testDeltasAccessibleByBeginVersion() throws Exception {
    appendDeltas(d1, d2, d3);
    assertEquals(d1.transformed, target.getTransformedDelta(V0));
    assertEquals(d1.applied, target.getAppliedDelta(V0));

    assertEquals(d2.transformed, target.getTransformedDelta(d1.getResultingVersion()));
    assertEquals(d2.applied, target.getAppliedDelta(d1.getResultingVersion()));

    assertEquals(d3.transformed, target.getTransformedDelta(d2.getResultingVersion()));
    assertEquals(d3.applied, target.getAppliedDelta(d2.getResultingVersion()));

    // Wrong hashes return null.
    assertNull(target.getTransformedDelta(HashedVersion.unsigned(0)));
    assertNull(target.getAppliedDelta(HashedVersion.unsigned(0)));
  }

  public void testDeltasAccesssibleByEndVersion() throws Exception {
    appendDeltas(d1, d2, d3);
    for (DeltaFacets d : Arrays.asList(d1, d2, d3)) {
      assertEquals(d.transformed,
          target.getTransformedDeltaByEndVersion(d.getResultingVersion()));
      assertEquals(d.applied,
          target.getAppliedDeltaByEndVersion(d.getResultingVersion()));
    }

    // Wrong hashes return null.
    assertNull(target.getTransformedDeltaByEndVersion(
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
    assertNull(target.getAppliedDeltaByEndVersion(
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
  }

  public void testDeltaHistoryRequiresCorrectHash() throws Exception {
    appendDeltas(d1);

    // Wrong start hash.
    assertNull(target.getTransformedDeltaHistory(HashedVersion.unsigned(0),
        d1.getResultingVersion()));
    assertNull(target.getAppliedDeltaHistory(HashedVersion.unsigned(0),
        d1.getResultingVersion()));

    // Wrong end hash.
    assertNull(target.getTransformedDeltaHistory(V0,
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
    assertNull(target.getAppliedDeltaHistory(V0,
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
  }

  public void testSingleDeltaHistoryAccessible() throws Exception {
    appendDeltas(d1);
    DeltaSequence transformedHistory = target.getTransformedDeltaHistory(V0,
        d1.getResultingVersion());
    assertNotNull(transformedHistory);
    assertEquals(1, transformedHistory.size());
    assertEquals(d1.transformed, transformedHistory.get(0));

    Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedHistory =
        target.getAppliedDeltaHistory(V0, d1.getResultingVersion());
    assertNotNull(appliedHistory);
    assertEquals(1, appliedHistory.size());
    assertEquals(d1.applied, Iterables.getOnlyElement(appliedHistory));
  }

  public void testDeltaHistoryQueriesCorrectHistory() throws Exception {
    appendDeltas(d1, d2, d3);

    checkHistoryForDeltas(d1);
    checkHistoryForDeltas(d1, d2);
    checkHistoryForDeltas(d2, d3);
    checkHistoryForDeltas(d1, d2, d3);
  }

  /**
   * Checks that a request for the deltas spanning a contiguous sequence of
   * delta facets produces correct results.
   */
  private void checkHistoryForDeltas(DeltaFacets... deltas) {
    HashedVersion beginVersion = deltas[0].appliedAtVersion;
    HashedVersion endVersion = deltas[deltas.length - 1].transformed.getResultingVersion();

    {
      List<TransformedWaveletDelta> expected = Lists.newArrayListWithExpectedSize(deltas.length);
      for (DeltaFacets d : deltas) {
        expected.add(d.transformed);
      }
      assertEquals(expected, target.getTransformedDeltaHistory(beginVersion, endVersion));
    }
    {
      List<ByteStringMessage<ProtocolAppliedWaveletDelta>> expected =
          Lists.newArrayListWithExpectedSize(deltas.length);
      for (DeltaFacets d : deltas) {
        expected.add(d.applied);
      }
      assertTrue(Iterables.elementsEqual(expected,
          target.getAppliedDeltaHistory(beginVersion, endVersion)));
    }
  }

  public void testPersistenceListenerNotCalledBeforePersistence() throws Exception {
    PersistenceListener listener = mock(PersistenceListener.class);
    target.addPersistenceListener(listener);
    appendDeltas(d1);
    awaitPersistence();
    Mockito.verifyZeroInteractions(listener);
  }

  public void checkListenerReceivesCallbackForSingleDelta() throws Exception {
    PersistenceListener listener = mock(PersistenceListener.class);
    target.addPersistenceListener(listener);
    appendDeltas(d1);
    target.persist(d1.getResultingVersion());
    awaitPersistence();
    Mockito.verify(listener).persisted(NAME, d1.getResultingVersion());
  }

  public void checkListenerReceivesCallbackForManyDeltas() throws Exception {
    PersistenceListener listener = mock(PersistenceListener.class);
    target.addPersistenceListener(listener);
    appendDeltas(d1, d2, d3);
    target.persist(d3.getResultingVersion());
    awaitPersistence();
    Mockito.verify(listener).persisted(NAME, d3.getResultingVersion());
    // There may have been other calls too, which the mock will ignore.
  }

  public void testPersistIsIdempotent() throws Exception {
    PersistenceListener listener = mock(PersistenceListener.class);
    appendDeltas(d1);
    target.persist(d1.getResultingVersion());
    awaitPersistence();

    target.addPersistenceListener(listener);
    target.persist(d1.getResultingVersion());
    awaitPersistence();
    Mockito.verifyZeroInteractions(listener);
  }

  public void testCanPersistOnlySomeDeltas() throws Exception {
    PersistenceListener listener = mock(PersistenceListener.class);
    target.addPersistenceListener(listener);
    appendDeltas(d1, d2, d3);
    target.persist(d2.getResultingVersion());
    awaitPersistence();
    Mockito.verify(listener).persisted(NAME, d2.getResultingVersion());

    target.persist(d3.getResultingVersion());
    awaitPersistence();
    Mockito.verify(listener).persisted(NAME, d3.getResultingVersion());
  }

  /**
   * Applies a delta to the target.
   */
  private void appendDeltas(DeltaFacets... deltas) throws InvalidProtocolBufferException,
      OperationException {
    for (DeltaFacets delta : deltas) {
      target.appendDelta(delta.appliedAtVersion, delta.transformed, delta.applied);
    }
  }

  /**
   * Creates a delta of no-ops and builds the corresponding applied and
   * transformed delta objects.
   */
  private static DeltaFacets makeDelta(HashedVersion appliedAtVersion, long timestamp, int numOps)
      throws InvalidProtocolBufferException {
    // Use no-op delta so the ops can actually apply.
    WaveletDelta delta = UTIL.makeNoOpDelta(appliedAtVersion, timestamp, numOps);
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        WaveServerTestUtil.buildAppliedDelta(delta, timestamp);
    TransformedWaveletDelta transformedDelta =
        AppliedDeltaUtil.buildTransformedDelta(appliedDelta, delta);
    return new DeltaFacets(appliedAtVersion, appliedDelta, transformedDelta);
  }
}
