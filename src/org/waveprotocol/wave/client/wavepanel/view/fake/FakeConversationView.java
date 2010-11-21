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
package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.wavepanel.view.ConversationView;

/**
 * Fake, pojo implementation of a conversation view.
 *
 */
public abstract class FakeConversationView implements ConversationView {
  protected final FakeRootThreadView thread;

  public FakeConversationView() {
    this.thread = new FakeRootThreadView(this);
  }

  @Override
  public FakeRootThreadView getRootThread() {
    return thread;
  }

  void remove(FakeRootThreadView thread) {
    throw new RuntimeException("Can not remove a thread from its conversation");
  }
}
