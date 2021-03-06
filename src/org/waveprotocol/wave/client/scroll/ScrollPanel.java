/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.wave.client.scroll;


/**
 * Reveals the scrolling capabilities of a view container.
 *
 * @param <T> type of views within this container
 */
public interface ScrollPanel<T> {
  /**
   * Moves the viewport so that {@code location} is at the viewport top.
   *
   * @param location content-space location to appear at the top of the
   *        viewport.
   */
  void moveTo(double location);

  /** @return the viewport extent of this scroller. */
  Extent getViewport();

  /** @return the content extent of this scroller. */
  Extent getContent();

  /** @return the extent of a view within this panel. */
  Extent extentOf(T view);

}
