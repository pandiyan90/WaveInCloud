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

package org.waveprotocol.wave.examples.fedone.robots.passive;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.common.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.robots.OperationContext;
import org.waveprotocol.wave.examples.fedone.robots.OperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.RobotCapabilities;
import org.waveprotocol.wave.examples.fedone.robots.operations.OperationService;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link RobotOperationApplicator}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotOperationApplicatorTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId ROBOT_PARTICIPANT =
      ParticipantId.ofUnsafe("robot@example.com");
  private static final RobotAccountData ACCOUNT =
      new RobotAccountDataImpl(ROBOT_PARTICIPANT, "www.example.com", "secret",
          new RobotCapabilities(
              Maps.<EventType, Capability> newHashMap(), "fake", ProtocolVersion.DEFAULT), true);

  private EventDataConverterManager converterManager;
  private WaveletProvider waveletProvider;
  private OperationServiceRegistry operationRegistry;
  private RobotOperationApplicator applicator;
  private ObservableWaveletData waveletData;
  private HashedVersion hashedVersionZero;

  @Override
  protected void setUp() {
    converterManager = mock(EventDataConverterManager.class);
    waveletProvider = mock(WaveletProvider.class);
    operationRegistry = mock(OperationServiceRegistry.class);
    EventDataConverter converter = mock(EventDataConverter.class);

    ConversationUtil conversationUtil = mock(ConversationUtil.class);
    applicator = new RobotOperationApplicator(
        converterManager, waveletProvider, operationRegistry, conversationUtil);

    waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ROBOT_PARTICIPANT, 0L);
    hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
  }

  public void testapplyOperationsExecutesAndSubmitsDelta() throws Exception {
    // Use a special operation service that generates a simple delta;
    OperationService service = new OperationService() {
      @Override
      public void execute(
          OperationRequest operation, OperationContext context, ParticipantId participant)
          throws InvalidRequestException {
        context.openWavelet(WAVE_ID, WAVELET_ID, ROBOT_PARTICIPANT).addParticipant(ALEX);
      }
    };
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    OperationRequest operation = new OperationRequest("wavelet.create", "op1");
    List<OperationRequest> operations = Collections.singletonList(operation);

    applicator.applyOperations(operations, waveletData, hashedVersionZero, ACCOUNT);

    verify(operationRegistry).getServiceFor(any(OperationType.class));
    verify(waveletProvider).submitRequest(eq(WAVELET_NAME), any(ProtocolWaveletDelta.class),
        any(WaveletProvider.SubmitRequestListener.class));
  }

  public void testGetOperationAuthor() throws Exception {
    // Type of operation doesn't matter
    OperationRequest operation = new OperationRequest("wavelet.create", "op1");
    ParticipantId author = applicator.getOperationAuthor(operation, ACCOUNT);
    assertEquals(author, ROBOT_PARTICIPANT);

    String proxy = "proxy";
    operation = new OperationRequest(
        "wavelet.create", "op1", Parameter.of(ParamsProperty.PROXYING_FOR, proxy));
    ParticipantId proxyAuthor = applicator.getOperationAuthor(operation, ACCOUNT);

    ParticipantId proxyParticipant = ParticipantId.of("robot+" + proxy + "@example.com");
    assertEquals(proxyAuthor, proxyParticipant);
  }

  public void testGetOperationAuthorThrowsException() {
    // Type of operation doesn't matter
    OperationRequest operation = new OperationRequest(
        "wavelet.create", "op1", Parameter.of(ParamsProperty.PROXYING_FOR, "invalid#$%^"));
    try {
      applicator.getOperationAuthor(operation, ACCOUNT);
      fail("Expected InvalidParticpantAddress since proxy in malformed");
    } catch (InvalidParticipantAddress e) {
      // Expected
    }
  }
}
