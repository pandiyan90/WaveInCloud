/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.model.wave.data;

import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * A document factory that produces a document operation sink which will raise a
 * runtime exception on any outgoing mutation ops.
 *
 * A {@link DocumentOperationSink} produced by this factory provides an
 * {@link ObservableDocument}.
 *
 * @author anorth@google.com (Alex North)
 */
public class ReadOnlyDocumentFactory implements DocumentFactory<DocumentOperationSink> {

  private final DocumentFactory<? extends ObservablePluggableMutableDocument> factory;

  public ReadOnlyDocumentFactory(SchemaProvider schemas) {
    this.factory = ObservablePluggableMutableDocument.createFactory(schemas);
  }

  @Override
  public DocumentOperationSink create(WaveletId waveletId, String docId,
      DocInitialization content) {
    ObservablePluggableMutableDocument doc = factory.create(waveletId, docId, content);
    doc.init(SilentOperationSink.BLOCKED);
    return doc;
  }
}
