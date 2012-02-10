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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of wavelets, local and remote, held in memory.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class WaveMap {

  /**
   * Returns a future whose result is the ids of stored wavelets in the given wave.
   * Any failure is reported as a {@link PersistenceException}.
   */
  private static ListenableFuture<ImmutableSet<WaveletId>> lookupWavelets(
      final WaveId waveId, final WaveletStore<?> waveletStore, Executor lookupExecutor) {
    ListenableFutureTask<ImmutableSet<WaveletId>> task =
        new ListenableFutureTask<ImmutableSet<WaveletId>>(
            new Callable<ImmutableSet<WaveletId>>() {
              @Override
              public ImmutableSet<WaveletId> call() throws PersistenceException {
                return waveletStore.lookup(waveId);
              }
            });
    lookupExecutor.execute(task);
    return task;
  }

  private static Logger LOG = Logger.getLogger(WaveMap.class.getName());

  private final ConcurrentMap<WaveId, Wave> waves;
  private final WaveletStore<?> store;

  @Inject
  public WaveMap(final DeltaAndSnapshotStore waveletStore,
      final WaveletNotificationSubscriber notifiee,
      WaveBus dispatcher,
      final LocalWaveletContainer.Factory localFactory,
      final RemoteWaveletContainer.Factory remoteFactory,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) final String waveDomain,
      WaveDigester digester) {
    // NOTE(anorth): DeltaAndSnapshotStore is more specific than necessary, but
    // helps Guice out.
    // TODO(soren): inject a proper executor (with a pool of configurable size)
    this.store = waveletStore;
    final Executor lookupExecutor = Executors.newSingleThreadExecutor();
    waves = new MapMaker().makeComputingMap(new Function<WaveId, Wave>() {
      @Override
      public Wave apply(WaveId waveId) {
        ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets =
            lookupWavelets(waveId, waveletStore, lookupExecutor);
        return new Wave(waveId, lookedupWavelets, notifiee, localFactory, remoteFactory,
            waveDomain);
      }
    });
  }

  /**
   * Loads all wavelets from storage.
   *
   * @throws WaveletStateException if storage access fails.
   */
  public void loadAllWavelets() throws WaveletStateException {
    try {
      ExceptionalIterator<WaveId, PersistenceException> itr = store.getWaveIdIterator();
        while (itr.hasNext()) {
          WaveId waveId = itr.next();
          lookupWavelets(waveId);
        }
    } catch (PersistenceException e) {
      throw new WaveletStateException("Failed to scan waves", e);
    } catch (java.lang.NegativeArraySizeException e) {
      LOG.log(Level.SEVERE, "Something bad happened while scanning for waves", e);
    }
  }

  public ExceptionalIterator<WaveId, WaveServerException> getWaveIds() {
    Iterator<WaveId> inner = waves.keySet().iterator();
    return ExceptionalIterator.FromIterator.create(inner);
  }

  public ImmutableSet<WaveletId> lookupWavelets(WaveId waveId) throws WaveletStateException {
    ListenableFuture<ImmutableSet<WaveletId>> future = waves.get(waveId).getLookedupWavelets();
    try {
      return FutureUtil.getResultOrPropagateException(future, PersistenceException.class);
    } catch (PersistenceException e) {
      throw new WaveletStateException("Failed to look up wave " + waveId, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WaveletStateException("Interrupted while looking up wave " + waveId, e);
    } catch (Exception e) {
      LOG.severe("Problem while looking up wavelets for " + waveId);
      return ImmutableSet.of();
    }
  }

  public LocalWaveletContainer getLocalWavelet(WaveletName waveletName)
      throws WaveletStateException {
    return waves.get(waveletName.waveId).getLocalWavelet(waveletName.waveletId);
  }

  public RemoteWaveletContainer getRemoteWavelet(WaveletName waveletName)
      throws WaveletStateException {
    return waves.get(waveletName.waveId).getRemoteWavelet(waveletName.waveletId);
  }

  public LocalWaveletContainer getOrCreateLocalWavelet(WaveletName waveletName) {
    return waves.get(waveletName.waveId).getOrCreateLocalWavelet(waveletName.waveletId);
  }

  public RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletName waveletName) {
    return waves.get(waveletName.waveId).getOrCreateRemoteWavelet(waveletName.waveletId);
  }
}
