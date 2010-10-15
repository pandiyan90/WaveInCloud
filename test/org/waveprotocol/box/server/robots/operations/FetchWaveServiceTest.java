/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.server.robots.operations;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;

import junit.framework.TestCase;

import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Unit tests for {@link FetchWaveService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class FetchWaveServiceTest extends TestCase {

  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example.com!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final String OPERATION_ID = "op1";
  private static final String FETCH_OP_NAME = OperationType.ROBOT_FETCH_WAVE.method();

  private FetchWaveService service;
  private OperationServiceHelper helper;

  @Override
  protected void setUp() throws Exception {
    service = FetchWaveService.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ALEX);
  }

  public void testFetchWave() throws Exception {
    String message = "A message";
    OperationRequest operation =
        new OperationRequest(FETCH_OP_NAME, OPERATION_ID, WAVE_ID, WAVELET_ID,
            Parameter.of(ParamsProperty.MESSAGE, message));
    OperationContextImpl context = helper.getContext();

    service.execute(operation, context, ALEX);

    ObservableConversation conversation =
        context.getConversation(context.openWavelet(WAVE_ID, WAVELET_ID, ALEX)).getRoot();

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertNotNull("expected a response", response);
    assertFalse("expected a success response", response.isError());
    assertEquals("Expected the response to carry the message", message,
        response.getData().get(ParamsProperty.MESSAGE));
    assertNotNull("Expected the response to carry a wavelet",
        response.getData().get(ParamsProperty.WAVELET_DATA));
    assertEquals("Expected the response to carry root blip id",
        conversation.getRootThread().getFirstBlip().getId(),
        response.getData().get(ParamsProperty.BLIP_ID));
  }

  public void testFetchWaveWithMissingParamThrowsInvalidRequestException() throws Exception {
    // No wave id or wavelet id set.
    OperationRequest operation = new OperationRequest(FETCH_OP_NAME, OPERATION_ID);
    OperationContextImpl context = helper.getContext();

    try {
      service.execute(operation, context, ALEX);
      fail("Expected InvalidRequestException because params were not set");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

}
