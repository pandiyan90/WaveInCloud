// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A document listener that is only interested in element events.
 *
*
 * @param <E> document's element type
 *
 *        NOTE(koz/hearnden): There is a problem inherent in this interface in
 *        that element add/removes are handled one at a time, when in actuality
 *        they occur in groups. This atomicity mismatch between the document and
 *        the listener means that implementors of this interface don't know,
 *        after any given call, whether or not the document was ever actually in
 *        the state that they are witnessing. For example, if a document has
 *        three nodes and they are all deleted in one change, then a listener
 *        that is keeping track of additions/removals of elements will 'see' the
 *        document as having all three children, then having two children, then
 *        having one child and then none. If the listener responds to the
 *        document changing by, say, printing out a representation of each
 *        element in the tree, then after the first element gets removed it will
 *        attempt to dereference the other two elements, which is invalid
 *        (because they have already been removed) and will cause an exception
 *        to be thrown.
 */
public interface ElementListener<E> {
  /**
   * Notifies this listener that an element within a document has been added.
   *
   * @param element element that changed
   */
  void onElementAdded(E element);

  /**
   * Notifies this listener that an element within a document has been removed.
   *
   * @param element element that changed
   */
  void onElementRemoved(E element);
}
