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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.collect.Lists;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.event.WaveletCreatedEvent;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.List;

/**
 * Implementation of the "wavelet.create" and "robot.createwavelet" operations.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class CreateWaveletService implements OperationService {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);

  private CreateWaveletService() {
  }

  /**
   * Creates a new wavelet that is conversational based on the given
   * {@link WaveletData}. Note that this is the robot api version of wavelet
   * data not to be confused with
   * {@link org.waveprotocol.wave.model.wave.data.WaveletData}.
   *
   * <p>
   * The wavelet data must define the wave id in the temporary format otherwise
   * it can not be opened by subsequent operations when calling
   * {@link OperationContext#openWavelet(String, String, ParticipantId)} with
   * the temporary wave id and wavelet id.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @throws InvalidRequestException if the operation fails to perform.
   */
  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    WaveletData waveletData =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_DATA);

    // The loop validates the addresses present in the wavelet data before
    // creating a new wavelet.
    List<ParticipantId> participants = Lists.newArrayList(participant);
    for (String address : waveletData.getParticipants()) {
      try {
        participants.add(ParticipantId.of(address));
      } catch (InvalidParticipantAddress e) {
        throw new InvalidRequestException(
            address + " is not a valid participant address", operation);
      }
    }

    WaveletName waveletName = context.getConversationUtil().generateWaveletName();
    ObservableWaveletData emptyWavelet =
        WaveletDataUtil.createEmptyWavelet(waveletName, participant, System.currentTimeMillis());
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(waveletName);

    RobotWaveletData newWavelet = new RobotWaveletData(emptyWavelet, hashedVersionZero);
    OpBasedWavelet opBasedWavelet = newWavelet.getOpBasedWavelet(participant);

    WaveletBasedConversation.makeWaveletConversational(opBasedWavelet);

    ObservableConversationView conversation = context.getConversation(opBasedWavelet);
    ObservableConversationBlip rootBlip = conversation.getRoot().getRootThread().appendBlip();

    for (ParticipantId newParticipant : participants) {
      opBasedWavelet.addParticipant(newParticipant);
    }

    // Store the temporary id of the wavelet and rootblip so that future
    // operations can reference it.
    context.putWavelet(waveletData.getWaveId(), waveletData.getWaveletId(), newWavelet);
    context.putBlip(waveletData.getRootBlipId(), rootBlip);

    String message = OperationUtil.getOptionalParameter(operation, ParamsProperty.MESSAGE);
    WaveletCreatedEvent event =
        new WaveletCreatedEvent(null, null, participant.getAddress(), System.currentTimeMillis(),
            rootBlip.getId(), message, waveletName.waveId.serialise(),
            waveletName.waveletId.serialise());
    context.processEvent(operation, event);
  }

  public static CreateWaveletService create() {
    return new CreateWaveletService();
  }
}