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
package org.waveprotocol.wave.client.concurrencycontrol;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.wave.ContentDocumentSinkFactory;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext.Factory;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Collection;

/**
 * Makes passive wavelet objects ({@link ObservableWaveletData}) operational,
 * turning them into mutable wavelet objects {@link OpBasedWavelet} backed by
 * operation sinks.
 *
 */
public final class WaveletOperationalizer {

  private final WaveId waveId;
  private final StringMap<LiveTarget<ObservableWaveletData, WaveletOperation>> wavelets =
      CollectionUtils.createStringMap();
  private final ContentDocumentSinkFactory docRegistry;
  private final WaveletOperationContext.Factory opContextFactory;

  private WaveletOperationalizer(
      WaveId waveId, ContentDocumentSinkFactory docRegistry, Factory opContextFactory) {
    this.waveId = waveId;
    this.docRegistry = docRegistry;
    this.opContextFactory = opContextFactory;
  }

  /**
   * Creates an operationalizer.
   */
  public static WaveletOperationalizer create(
      WaveId wave, ContentDocumentSinkFactory docRegistry, ParticipantId user) {
    WaveletOperationContext.Factory opContexts = new BasicWaveletOperationContextFactory(user);
    return new WaveletOperationalizer(wave, docRegistry, opContexts);
  }

  /**
   * Turns a passive wavelet into a mutable wavelet, under the control of
   * operations.
   * <p>
   * Note that this does not connect the wavelet with concurrency control, which
   * means that local mutations will not be sent out anywhere, and remote
   * mutations will not be routed to this wavelet. Additional work needs to be
   * done for that (see {@link WaveChannelBinder} for that); this method merely
   * associates the passive data wavelet with operation sinks that make it
   * locally mutable. It is safe to mutate the returned wavelet before binding
   * it with an operation channel; local mutations that occur before binding are
   * queued until bound.
   *
   * @param data data object for the wavelet
   * @return mutable operation-backed wavelet.
   */
  public OpBasedWavelet operationalize(ObservableWaveletData data) {
    LiveTarget<ObservableWaveletData, WaveletOperation> target = createSinks(data);
    OpBasedWavelet wavelet = new OpBasedWavelet(waveId,
        data,
        opContextFactory,
        ParticipationHelper.IGNORANT,
        target.getExecutorSink(),
        target.getOutputSink());
    docRegistry.registerOpBasedWavelet(wavelet);
    return wavelet;
  }

  /** @return all the operation-controlled targets in this wave. */
  public Collection<ObservableWaveletData> getWavelets() {
    final Collection<ObservableWaveletData> targets = CollectionUtils.createQueue();
    this.wavelets.each(new ProcV<LiveTarget<ObservableWaveletData, WaveletOperation>>() {
      @Override
      public void apply(String id, LiveTarget<ObservableWaveletData, WaveletOperation> triple) {
        targets.add(triple.getTarget());
      }
    });
    return targets;
  }

  /** @return the input and output sinks for a particular wavelet. */
  public Pair<SilentOperationSink<WaveletOperation>, ProxyOperationSink<WaveletOperation>> getSinks(
      String waveletId) {
    LiveTarget<ObservableWaveletData, WaveletOperation> target = wavelets.get(waveletId);
    return Pair.of(target.getExecutorSink(), target.getOutputSink());
  }

  /**
   * Creates a liveness triple for a data object, storing the triple in a map.
   */
  private LiveTarget<ObservableWaveletData, WaveletOperation> createSinks(
      ObservableWaveletData data) {
    return putAndReturn(wavelets, data.getWaveletId().serialise(),
        LiveTarget.<ObservableWaveletData, WaveletOperation>create(data));
  }

  // Saves a bit of typing...
  private static <V> V putAndReturn(StringMap<V> map, String key, V value) {
    Preconditions.checkState(!map.containsKey(key));
    map.put(key, value);
    return value;
  }
}