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

package org.waveprotocol.wave.client.editor.content.misc;

import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.paragraph.DefaultParagraphHtmlRenderer;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphRenderer;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.ElementHandlerRegistry;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * A caption is a {@link LinoTextEventHandler} that has its own contentEditable attribute,
 * and can therefore be used inside non-editable areas.
 *
 * See {@link org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail} or
 * {@code Button} for examples
 */
public class Caption {

  public static final String FULL_TAGNAME = "w:caption";

  public static final NodeEventHandler CAPTION_EVENT_HANDLER = new LinoTextEventHandler() {
    @Override
    public void onActivated(ContentElement element) {
      super.onActivated(element);
      DisplayEditModeHandler.setEditModeListener(element, UpdateContentEditable.get());
    }
  };

  /**
   * Registers subclass with ContentElement
   */
  public static void register(ElementHandlerRegistry registry) {
    registry.register(NodeEventHandler.class, FULL_TAGNAME, CAPTION_EVENT_HANDLER);
    registry.register(NodeMutationHandler.class, FULL_TAGNAME, Paragraph.DEFAULT_RENDERER);
    // TODO(danilatos): Stop using non-html tags
    registry.register(Renderer.class, FULL_TAGNAME, new ParagraphRenderer(
        new DefaultParagraphHtmlRenderer(FULL_TAGNAME)));
  }


//  TODO(danilatos): Bring this functionality back
//  /**
//   * {@inheritDoc}
//   */
//  public void onEditorModeChange(boolean editing) {
//    getImplNodelet().setAttribute("contentEditable", editing ? "true" : "false");
//  }

  /**
   * @param caption
   * @return A content xml string containing a caption
   */
  public static XmlStringBuilder constructXml(XmlStringBuilder caption) {
    return caption.wrap(FULL_TAGNAME);
  }

  /**
   * @param node
   * @return true if this node is a caption node
   */
  public static boolean isCaption(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, FULL_TAGNAME);
  }
}
