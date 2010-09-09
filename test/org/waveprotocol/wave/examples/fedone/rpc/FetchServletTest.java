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

package org.waveprotocol.wave.examples.fedone.rpc;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.MessageLite;

import junit.framework.TestCase;

import org.waveprotocol.wave.common.util.JavaWaverefEncoder;
import org.waveprotocol.wave.examples.fedone.common.SnapshotSerializer;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for the FetchServlet. The fetch servlet provides wavelet snapshots
 * from a waveletProvider.
 * 
 * These tests make sure reasonable errors are generated for invalid URLs and
 * that the fetch results match what was sent.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FetchServletTest extends TestCase {
  private ProtoSerializer protoSerializer = new ProtoSerializer();
  private WaveletProviderStub waveletProvider;
  private FetchServlet servlet;

  @Override
  protected void setUp() throws Exception {
    // Its important that we reset the wavelet provider between tests.
    waveletProvider = new WaveletProviderStub();
    servlet = new FetchServlet(waveletProvider, protoSerializer);
  }
  
  public void testGetInvalidWaverefReturnsNotFound() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    when(request.getPathInfo()).thenReturn("/invalidwaveref");
    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND);
  }
  
  /**
   * Fetch the given waveref from the servlet.
   * @param waveref
   * @param response
   * @throws Exception
   */
  protected void requestWaveRef(WaveRef waveref, HttpServletResponse response) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getPathInfo()).thenReturn("/" + JavaWaverefEncoder.encodeToUriPathSegment(waveref));
    servlet.doGet(request, response);
  }
  
  protected void verifyServletReturnsForbiddenForWaveref(WaveRef waveref) throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    requestWaveRef(waveref, response);
    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
  
  public void testGetMissingDataReturnsForbidden() throws Exception {
    WaveletData wavelet = waveletProvider.getHostedWavelet();
    WaveId waveId = wavelet.getWaveId();
    WaveletId waveletId = wavelet.getWaveletId();
    
    WaveRef unknownWave = WaveRef.of(new WaveId(waveId.getDomain(), waveId.getId() + "junk"));
    verifyServletReturnsForbiddenForWaveref(unknownWave);
    WaveRef unknownWavelet = WaveRef.of(waveId, new WaveletId(waveletId.getDomain(), waveletId.getId() + "junk"));
    verifyServletReturnsForbiddenForWaveref(unknownWavelet);
    WaveRef unknownDocument = WaveRef.of(waveId, waveletId, "madeupdocid");
    verifyServletReturnsForbiddenForWaveref(unknownDocument);
  }
  
  public String getFetchResultString(WaveRef waveref) throws Exception {
    WaveletData wavelet = waveletProvider.getHostedWavelet();
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));
    
    requestWaveRef(waveref, response);
    
    verify(response).getWriter();
    verify(response, never()).sendError(anyInt());
    
    return writer.toString();
  }
  
  public <T extends MessageLite> T getFetchResult(WaveRef waveref, Class<T> klass) throws Exception {
    String message = getFetchResultString(waveref);
    StringReader reader = new StringReader(message);
    return protoSerializer.parseFrom(reader, klass);
  }

  private static void checkSerializedDocumentPropertiesMatch(BlipData doc1, BlipData doc2) {
    assertNotNull(doc1);
    assertNotNull(doc2);
    
    assertEquals(doc1.getId(), doc2.getId());
    // XXXXXXXX how do I compare the operations???
    
    assertEquals(doc1.getAuthor(), doc2.getAuthor());
    assertEquals(doc1.getContributors(), doc2.getContributors());
    assertEquals(doc1.getLastModifiedTime(), doc2.getLastModifiedTime());
    assertEquals(doc1.getLastModifiedVersion(), doc2.getLastModifiedVersion());
  }
  
  private static void checkSerializedWaveletsMatch(WaveletData wavelet1, WaveletData wavelet2) {
    assertNotNull(wavelet1);
    assertNotNull(wavelet2);
    
    assertEquals(wavelet1.getWaveId(), wavelet2.getWaveId());
    assertEquals(wavelet1.getParticipants(), wavelet2.getParticipants());
    assertEquals(wavelet1.getVersion(), wavelet2.getVersion());
    assertEquals(wavelet1.getLastModifiedTime(), wavelet2.getLastModifiedTime());
    assertEquals(wavelet1.getCreator(), wavelet2.getCreator());
    assertEquals(wavelet1.getCreationTime(), wavelet2.getCreationTime());
    
    // & check that the documents the wavelets contain are also the same.
    assertEquals(wavelet1.getDocumentIds(), wavelet2.getDocumentIds());
    for (String docId : wavelet1.getDocumentIds()) {
      checkSerializedDocumentPropertiesMatch(wavelet1.getBlip(docId), wavelet2.getBlip(docId));
    }
  }
  
  public void testGetWavelet() throws Exception {
    // This test round-trips a wavelet and when it gets the response back, it
    // makes sure all the fields it cares about match up with the wavelet which
    // was sent.
    
    WaveletData wavelet1 = waveletProvider.getHostedWavelet();

    WaveRef waveref = WaveRef.of(wavelet1.getWaveId(), wavelet1.getWaveletId());
    WaveletSnapshot snapshot = getFetchResult(waveref, WaveletSnapshot.class);
    WaveletData wavelet2 = SnapshotSerializer.deserializeWavelet(snapshot, waveref.getWaveId());
    
    // We have just round-tripped wavelet1 through the servlet to get wavelet2.
    // They should be pretty much identical in all the fields that get
    // serialized.
    checkSerializedWaveletsMatch(wavelet1, wavelet2);
    
    // TODO(josephg): Enable this test when the persistence store is in place.
//    assertEquals(snapshot.getVersion(), waveletProvider.getCommittedVersion());
  }
  
  public void testGetDocument() throws Exception {
    // The fetch servlet also exposes document snapshots through a longer url
    // (/fetch/domain/waveid/domain/waveletid/docid). Fedone never uses this
    // API, but make sure its working.
    WaveletData wavelet = waveletProvider.getHostedWavelet();
    for (String docId : wavelet.getDocumentIds()) {
      // We currently have no way to deserialize a document. Instead, we'll
      // serialize the expected document and compare with what we get from the
      // fetch servlet.
      StringWriter writer = new StringWriter();
      BlipData expectedDoc = wavelet.getBlip(docId);
      protoSerializer.writeTo(writer, SnapshotSerializer.serializeDocument(expectedDoc));
      String expectedResult = writer.toString();
      
      WaveRef waveref = WaveRef.of(wavelet.getWaveId(), wavelet.getWaveletId(), docId);
      String actualResult = getFetchResultString(waveref);
      
      assertEquals(expectedResult, actualResult);
    }
  }
}
