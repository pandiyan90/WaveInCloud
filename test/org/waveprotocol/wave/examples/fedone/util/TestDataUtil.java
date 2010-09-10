// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;

/**
 * Some utility methods for manipulating wavelet data.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class TestDataUtil {
  /**
   * Create a wavelet data object for testing.
   * 
   * @return a simple wavelet.
   */
  public static WaveletData createSimpleWaveletData() {
    WaveletName name = WaveletName.of(new WaveId("example.com", "w+abc123"),
        new WaveletId("example.com", "conv+root"));
    ParticipantId creator = ParticipantId.ofUnsafe("sam@example.com");
    long time = 1234567890;
    
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(name, creator, time);

    DocInitialization content = new DocInitializationBuilder().characters("Hello there").build();
    wavelet.createBlip("b+abc123", creator, Collections.<ParticipantId>emptySet(), content, time, 0);
    
    return wavelet;
  }

  /**
   * Check that the serialized fields of two documents match one another.
   */
  public static void checkSerializedDocument(BlipData expected, BlipData actual) {
    assertNotNull(expected);
    assertNotNull(actual);
    
    assertEquals(expected.getId(), actual.getId());
    
    assertTrue(DocCompare.equivalent(DocCompare.STRUCTURE,
        expected.getContent().getMutableDocument(),
        actual.getContent().getMutableDocument()));
    
    assertEquals(expected.getAuthor(), actual.getAuthor());
    assertEquals(expected.getContributors(), actual.getContributors());
    assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
    assertEquals(expected.getLastModifiedVersion(), actual.getLastModifiedVersion());
  }
  
  /**
   * Check that the serialized fields of two wavelets are equal.
   */
  public static void checkSerializedWavelet(WaveletData expected, WaveletData actual) {
    assertNotNull(expected);
    assertNotNull(actual);
    
    assertEquals(expected.getWaveId(), actual.getWaveId());
    assertEquals(expected.getParticipants(), actual.getParticipants());
    assertEquals(expected.getVersion(), actual.getVersion());
    assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
    assertEquals(expected.getCreator(), actual.getCreator());
    assertEquals(expected.getCreationTime(), actual.getCreationTime());
    
    // & check that the documents the wavelets contain are also the same.
    assertEquals(expected.getDocumentIds(), actual.getDocumentIds());
    for (String docId : expected.getDocumentIds()) {
      checkSerializedDocument(expected.getBlip(docId), actual.getBlip(docId));
    }
  }
}