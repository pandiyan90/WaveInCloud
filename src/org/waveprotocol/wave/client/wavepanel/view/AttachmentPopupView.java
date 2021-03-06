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
 */

package org.waveprotocol.wave.client.wavepanel.view;

import org.waveprotocol.wave.media.model.AttachmentId;

/**
 * A link info popup.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public interface AttachmentPopupView {

  /**
   * Observer of view events.
   */
  public interface Listener {
    void onHide();

    void onShow();
    
    void onDone(String id, String fileName);
  }

  /**
   * Binds this view to a listener, until {@link #reset()}.
   */
  void init(Listener listener);

  /**
   * Releases this view from its listener, allowing it to be reused.
   */
  void reset();

  /**
   * Shows the popup.
   */
  void show();

  /**
   * Hides the popup.
   */
  void hide();
  
  void setAttachmentId(AttachmentId id);

}
