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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.v22.EventDataConverterV22;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.EventType;
import com.google.wave.api.event.WaveletParticipantsChangedEvent;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.RobotName;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.robots.util.WaveletPluginDocumentFactory;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.CapturingOperationSink;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.ConversionUtil;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Collections;
import java.util.Map;

/**
 * Unit tests for the {@link EventGenerator}.
 *
 * This class constructs an {@link OpBasedWavelet} on which the operations
 * performed are captured. These operations will later be used for in the
 * {@link EventGenerator}.
 *
 * The wavelet will have {@code ALEX} as participant and an empty root blip in
 * its conversation structure.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class EventGeneratorTest extends TestCase {

  private final static String ROBOT_ADDRESS = "robot@example.com";
  private final static RobotName ROBOT_NAME = RobotName.fromAddress(ROBOT_ADDRESS);
  private final static EventDataConverter CONVERTER = new EventDataConverterV22();
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final WaveletPluginDocumentFactory DOCUMENT_FACTORY =
      new WaveletPluginDocumentFactory(SchemaCollection.empty());
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example.com!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  private static final ParticipantId ROBOT = ParticipantId.ofUnsafe("robot@example.com");
  private static final BasicWaveletOperationContextFactory CONTEXT_FACTORY =
      new BasicWaveletOperationContextFactory(ALEX);

  /** Map containing a subscription to all possible events */
  private static final Map<EventType, Capability> ALL_CAPABILITIES;
  static {
    Builder<EventType, Capability> builder = ImmutableMap.builder();
    for (EventType event : EventType.values()) {
      if (!event.equals(EventType.UNKNOWN)) {
        builder.put(event, new Capability(event));
      }
    }
    ALL_CAPABILITIES = builder.build();
  }

  private EventGenerator eventGenerator;
  private HashedVersion versionZero;
  private ObservableWaveletData waveletData;
  private OpBasedWavelet wavelet;
  private CapturingOperationSink<WaveletOperation> output;

  @Override
  protected void setUp() throws Exception {
    ConversationUtil conversationUtil = new ConversationUtil(FakeIdGenerator.create());
    eventGenerator = new EventGenerator(ROBOT_NAME, conversationUtil);
    versionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);

    waveletData = WaveletDataImpl.Factory.create(DOCUMENT_FACTORY).create(
        new EmptyWaveletSnapshot(WAVELET_NAME.waveId, WAVELET_NAME.waveletId, ALEX, 0L));
    waveletData.addParticipant(ALEX);

    SilentOperationSink<WaveletOperation> executor =
        SilentOperationSink.Executor.build(waveletData);
    output = new CapturingOperationSink<WaveletOperation>();
    wavelet =
        new OpBasedWavelet(waveletData.getWaveId(), waveletData, CONTEXT_FACTORY,
            ParticipationHelper.IGNORANT, executor, output);
    DOCUMENT_FACTORY.setWavelet(wavelet);

    // Make a conversation and clear the sink
    WaveletBasedConversation.makeWaveletConversational(wavelet);
    conversationUtil.getConversation(wavelet).getRoot().getRootThread().appendBlip();
    output.clear();
  }

  // Actual Testing Code starts here ^_^

  public void testGenerateWaveletParticipantsChangedEventOnAdd() throws Exception {
    wavelet.addParticipant(BOB);
    EventMessageBundle messages = generateAndCheckEvents(EventType.WAVELET_PARTICIPANTS_CHANGED);
    assertTrue("Only expected one event", messages.getEvents().size() == 1);
    WaveletParticipantsChangedEvent event =
        WaveletParticipantsChangedEvent.as(messages.getEvents().get(0));
    assertTrue("Bob should be added", event.getParticipantsAdded().contains(BOB.getAddress()));
  }

  public void testGenerateWaveletParticipantsChangedEventOnRemove() throws Exception {
    wavelet.removeParticipant(ALEX);
    EventMessageBundle messages = generateAndCheckEvents(EventType.WAVELET_PARTICIPANTS_CHANGED);
    assertEquals("Only expected one event", 1, messages.getEvents().size());
    WaveletParticipantsChangedEvent event =
        WaveletParticipantsChangedEvent.as(messages.getEvents().get(0));
    assertTrue(
        "Alex should be removed", event.getParticipantsRemoved().contains(ALEX.getAddress()));
  }

  public void testGenerateWaveletSelfAddedEvent() throws Exception {
    wavelet.addParticipant(ROBOT);
    EventMessageBundle messages = generateAndCheckEvents(EventType.WAVELET_SELF_ADDED);
    assertEquals("Only expected two evenst", 2, messages.getEvents().size());
  }

  public void testGenerateWaveletSelfRemovedEvent() throws Exception {
    wavelet.addParticipant(ROBOT);
    wavelet.removeParticipant(ROBOT);
    EventMessageBundle messages = generateAndCheckEvents(EventType.WAVELET_SELF_REMOVED);
    assertEquals("Expected three events", 3, messages.getEvents().size());
  }

  // Helper Methods

  /**
   * Collects the ops applied to wavelet and creates a delta for processing in
   * the event generator.
   *
   * @param eventType the type of event that should have been generated.
   * @return the {@link EventMessageBundle} with the events generated when a
   *         robot is subscribed to all possible events.
   */
  private EventMessageBundle generateAndCheckEvents(EventType eventType) throws Exception {
    // Create the delta
    CoreWaveletDelta delta = ConversionUtil.toCoreWaveletDelta(output.getOps(), ALEX, versionZero);
    VersionedWaveletDelta versionedDelta = new VersionedWaveletDelta(delta, versionZero);
    WaveletAndDeltas waveletAndDeltas = WaveletAndDeltas.create(
        waveletData, Collections.singletonList(versionedDelta), versionZero);

    // Put the wanted event in the capabilities map
    Map<EventType, Capability> capabilities = Maps.newHashMap();
    capabilities.put(eventType, new Capability(eventType));

    // Generate the events
    EventMessageBundle messages =
        eventGenerator.generateEvents(waveletAndDeltas, capabilities, CONVERTER);

    // Check that the event was generated and that no other types were generated
    checkGeneratedEvent(messages, eventType);
    checkAllEventsAreInCapabilites(messages, capabilities);

    // Generate events with all capabilities
    messages = eventGenerator.generateEvents(waveletAndDeltas, ALL_CAPABILITIES, CONVERTER);
    checkGeneratedEvent(messages, eventType);

    return messages;
  }

  /**
   * Checks whether an event of the given type has been put in the bundle.
   */
  private void checkGeneratedEvent(EventMessageBundle messages, EventType type) {
    for (Event event : messages.getEvents()) {
      if (event.getType().equals(type)) {
        // Success
        return;
      }
    }
    fail("Event of type " + type + " has not been generated");
  }

  /**
   * Checks whether all events generated are in the capabilities map.
   */
  private void checkAllEventsAreInCapabilites(
      EventMessageBundle messages, Map<EventType, Capability> capabilities) {
    for (Event event : messages.getEvents()) {
      if (!capabilities.containsKey(event.getType())) {
        fail("Generated event of type" + event.getType() + " which is not in the capabilities");
      }
    }
  }
}
