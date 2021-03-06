/**
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
 */
package org.waveprotocol.box.server.rpc.render.view.builder;

import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.close;
import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.image;
import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.openWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.box.server.rpc.render.common.safehtml.EscapeUtils;
import org.waveprotocol.box.server.rpc.render.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.box.server.rpc.render.uibuilder.BuilderHelper.Component;
import org.waveprotocol.box.server.rpc.render.uibuilder.UiBuilder;
import org.waveprotocol.box.server.rpc.render.view.IntrinsicReplyBoxView;
import org.waveprotocol.box.server.rpc.render.view.TypeCodes;
import org.waveprotocol.box.server.rpc.render.view.View.Type;

/**
 * This class is the view builder for the reply box that exists at the end of a
 * root thread.
 */
public final class ReplyBoxViewBuilder implements UiBuilder, IntrinsicReplyBoxView {

  public interface Resources {
    Css css();
  }

  public interface Css {
    /** The main reply box container. */
    String replyBox();
    
    /** The avatar image. */
    String avatar();
  }

  /** An enum for all the components of a reply box view. */
  public enum Components implements Component {
    /** The avatar element. */
    AVATAR("A");

    private final String postfix;

    Components(String postfix) {
      this.postfix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + postfix;
    }
  }

  /** A unique id for this builder. */
  private final String id;
  
  /** The css resources for this class. */
  private final Css css;

  //
  // Intrinsic state.
  //

  /** The image url to the avatar of the logged in user. */
  private String avatarUrl;
  
  /** Specifies weather the reply box should be rendered as enabled or not. **/
  private boolean enabled = false;

  /**
   * Creates a new reply box view builder with the given id.
   * 
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   */
  public static ReplyBoxViewBuilder create(WavePanelResources resources, String id) {
    return new ReplyBoxViewBuilder(resources.getReplyBox().css(), id);
  }

  @VisibleForTesting
  ReplyBoxViewBuilder(Css css, String id) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    this.css = css;
    this.id = id;
    this.avatarUrl = "static/images/unknown.jpg";
  }

  @Override
  public void setAvatarImageUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  //
  // DomImpl nature.
  //

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    openWith(output, id, css.replyBox(), TypeCodes.kind(Type.REPLY_BOX),
        enabled ? "" : "style='display:none'");
    {
      // Author avatar.
      image(output, Components.AVATAR.getDomId(id), css.avatar(),
          EscapeUtils.fromString(avatarUrl), EscapeUtils.fromPlainText("author"), null);
      output.appendEscaped("Click here to reply");
    }
    close(output);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void enable() {
    setEnabled(true);
  }

  @Override
  public void disable() {
    setEnabled(false);
  }

  private void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}