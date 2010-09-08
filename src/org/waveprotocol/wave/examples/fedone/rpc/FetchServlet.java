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

import com.google.inject.Inject;
import com.google.protobuf.MessageLite;

import org.waveprotocol.wave.common.util.JavaWaverefEncoder;
import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletSnapshotBuilder;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for static fetching of wave data. Typically, the servlet will be
 * hosted on /fetch/*. A document, a wavelet, or a whole wave can be specified
 * in the URL.
 * 
 * Valid request formats are:
 * Fetch a wave:
 *  GET /fetch/wavedomain.com/waveid
 * Fetch a wavelet:
 *  GET /fetch/wavedomain.com/waveid/waveletdomain.com/waveletid
 * Fetch a document:
 *  GET /fetch/wavedomain.com/waveid/waveletdomain.com/waveletid/b+abc123
 *  
 * The format of the returned information is the protobuf-JSON format used by
 * the websocket interface. 
 */
public class FetchServlet extends HttpServlet {
  private static final Log LOG = Log.get(FetchServlet.class);
  
  @Inject
  public FetchServlet(WaveletProvider waveletProvider, ProtoSerializer serializer) {
    this.waveletProvider = waveletProvider;
    this.serializer = serializer;
  }
  
  protected ProtoSerializer serializer;
  protected WaveletProvider waveletProvider;

  /**
   * Get a snapshot from the wavelet provider of the given wavelet.
   * 
   * @param waveletName The name of the wavelet to fetch
   * @return A snapshot of the wavelet requested, or null if the wavelet doesn't
   * exist in the waveletProvider.
   */
  protected WaveletSnapshot getSnapshot(WaveletName waveletName) {
    WaveletSnapshotBuilder<WaveletSnapshot> snapshotBuilder =
      new WaveletSnapshotBuilder<WaveletSnapshot>() {
      @Override
      public WaveletSnapshot build(WaveletData waveletData, HashedVersion currentVersion,
          ProtocolHashedVersion committedVersion) {
        // Until the persistence store is in place, committedVersion will be
        // null. TODO(josephg): Remove this once the persistence layer works. 
        if (committedVersion == null) {
          committedVersion = CoreWaveletOperationSerializer.serialize(currentVersion);
        }
        
        // TODO(josephg): Also add a unit test for this in WaveletProvider.
        if (waveletData.getVersion() != committedVersion.getVersion()) {
          throw new RuntimeException("Provided snapshot version doesn't match committed version");
        }
        
        return WaveletOperationSerializer.serializeSnapshot(waveletData, committedVersion);
      }
    };
    return waveletProvider.getSnapshot(waveletName, snapshotBuilder);
  }
  
  private void serializeObjectToServlet(MessageLite message, HttpServletResponse dest)
        throws IOException {
    if (message == null) {
      dest.sendError(HttpServletResponse.SC_NOT_FOUND);
    } else {
      dest.setContentType("application/json");
      dest.setStatus(HttpServletResponse.SC_OK);
      
      serializer.writeTo(dest.getWriter(), message);
    }
  }
  
  /**
   * Render the requested waveref out to the HttpServletResponse dest.
   * 
   * @param waveref The referenced wave. Could be a whole wave, a wavelet or
   * just a document.
   * @param dest The servlet response to render the snapshot out to.
   * @throws IOException
   */
  protected void renderSnapshot(WaveRef waveref, HttpServletResponse dest) throws IOException {
    // TODO(josephg): Its currently impossible to fetch all wavelets inside a
    // wave that are visible to the user. Until this is fixed, if no wavelet is
    // specified we'll just return the conv+root.
    WaveletId waveletId = waveref.hasWaveletId() ?
        waveref.getWaveletId() : new WaveletId(waveref.getWaveId().getDomain(), "conv+root");
    
    WaveletName waveletName = WaveletName.of(waveref.getWaveId(), waveletId);
    LOG.info("Fetching snapshot of wavelet " + waveletName);
    WaveletSnapshot snapshot = getSnapshot(waveletName);
    
    if (snapshot != null) {
      if (waveref.hasDocumentId()) {
        // We have a wavelet id and document id. Find the document in the snapshot
        // and return it.
        DocumentSnapshot docSnapshot = null;
        for (DocumentSnapshot ds : snapshot.getDocumentList()) {
          if (ds.getDocumentId().equals(waveref.getDocumentId())) {
            docSnapshot = ds;
            break;
          }
        }
        serializeObjectToServlet(docSnapshot, dest);
      } else if (waveref.hasWaveletId()) {
        // We have a wavelet id. Pull up the wavelet snapshot and return it.
        serializeObjectToServlet(snapshot, dest);
      } else {
        // Wrap the conv+root we fetched earlier in a WaveSnapshot object and
        // send it.
        WaveSnapshot waveSnapshot = WaveSnapshot.newBuilder()
        .setWaveId(waveref.getWaveId().serialise())
        .addWavelet(snapshot).build();

        serializeObjectToServlet(waveSnapshot, dest);
      }
    } else {
      // Snapshot is null. 404.
      dest.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
  
  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response)
      throws IOException {
    
    // This path will look like "/google.com/w+abc123/foo.com/conv+root
    // Strip off the leading '/'.
    String urlPath = req.getPathInfo().substring(1);
    
    // Extract the name of the wavelet from the URL
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(urlPath);
    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    renderSnapshot(waveref, response);
  }
}
