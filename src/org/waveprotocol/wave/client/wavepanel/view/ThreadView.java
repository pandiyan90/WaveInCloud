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
package org.waveprotocol.wave.client.wavepanel.view;

import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * A view interface for a thread.
 *
 */
public interface ThreadView extends View, IntrinsicThreadView {

  View getParent();

  /**
   * Renders a blip into this thread.
   *
   * @param ref view of a thread item (either a blip or a conversation)
   * @param blip blip to render
   */
  BlipView insertBlipBefore(View ref, ConversationBlip blip);

  /**
   * @return the blip before {@code ref}, or the first blip if {@code ref} is
   *         {@code null}.
   */
  BlipView getBlipBefore(View ref);

  BlipView getBlipAfter(View ref);
  
  ThreadReplyIndicatorView getReplyIndicator();
}