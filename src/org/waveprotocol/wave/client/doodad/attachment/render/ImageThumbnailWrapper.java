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

package org.waveprotocol.wave.client.doodad.attachment.render;

import org.waveprotocol.wave.client.doodad.attachment.SimpleAttachmentManager;
import org.waveprotocol.wave.client.doodad.attachment.SimpleAttachmentManager.Attachment;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilderDoc;

/**
 * A per-instance wrapper to provide an interface over image thumbnail elements
 *
 * TODO(danilatos): Find a nice way to get rid of this?
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ImageThumbnailWrapper {
  /**
   * Property for storing an instance of this wrapper on a thumbnail element
   */
  static final Property<ImageThumbnailWrapper> PROPERTY = Property.immutable("wrapper");

  /** Element being wrapped. */
  private final ContentElement element;

  /** Attachment. */
  private final Attachment attachment;

  /**
   * @param element the element being wrapped
   * @param attachment the attachment
   */
  public ImageThumbnailWrapper(ContentElement element, Attachment attachment) {
    this.element = element;
    this.attachment = attachment;
  }

  /**
   * Gets the wrapper for an element, if it has one.
   *
   * @return wrapper for {@code e}.
   */
  public static ImageThumbnailWrapper of(ContentElement e) {
    return e.getProperty(ImageThumbnailWrapper.PROPERTY);
  }

  /**
   * @return attachment of the thumbnail.
   */
  public Attachment getAttachment() {
    return attachment;
  }

  /**
   * Get the text of the image caption
   *
   * @return string
   */
  public String getCaptionText() {
    CMutableDocument doc = element.getMutableDoc();
    return DocHelper.getText(doc, doc, doc.getLocation(element),
        doc.getLocation(Point.end((ContentNode) element)));
  }

  /**
   * Renders this wrapper's element into a string builder.
   *
   * @param builder  builder into which this wrapper's element is appended,
   *                 or null if this wrapper should produce a builder
   * @return resulting XML builder.
   */
  public XmlStringBuilderDoc<? super ContentElement, ContentElement, ?>
      appendInto(XmlStringBuilderDoc<? super ContentElement, ContentElement, ?> builder) {
    if (builder == null) {
      builder = XmlStringBuilderDoc.createEmpty(element.getMutableDoc());
    }
    builder.appendNode(element);
    return builder;
  }
}
