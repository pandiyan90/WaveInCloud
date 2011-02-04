/**
 * Copyright 2011 Google Inc.
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
package org.waveprotocol.box.webclient.search;

import org.waveprotocol.wave.client.widget.toolbar.GroupingToolbar;

/**
 * View interface for the search panel.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface SearchPanelView {

  /**
   * Receives gesture UI events.
   */
  interface Listener {
    void onClicked(DigestView digestUi);
  }

  /** Binds this view to a listener to handle UI gestures. */
  void init(Listener listener);

  /** Releases this view from its listener. */
  void reset();

  /** @return the search area. */
  SearchView getSearch();

  /** @return the search toolbar. */
  GroupingToolbar.View getToolbar();

  /** @return the digest view after another. */
  DigestView getNext(DigestView ref);

  /** @return the digest view before another. */
  DigestView getPrevious(DigestView ref);

  /** @return a rendering of {@code digest}. */
  DigestView insertBefore(DigestView ref, Digest digest);

  /** Removes all digest views. */
  void clearDigests();
}
