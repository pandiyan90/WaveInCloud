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
package org.waveprotocol.wave.client.editor;

import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.editor.content.RangeHelper;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Stack;

/**
 * Implementation of an EditorUndoManager.
 *
 * Keeps track of operations applied to a document and the selection at each
 * checkpoint, so that old states can be restored with undo/redo.
 *
 *
 */
public class EditorUndoManagerImpl implements EditorUndoManager {
  private final SilentOperationSink<BufferedDocOp> sink;
  private final SelectionHelper selectionHelper;

  private final Stack<FocusedRange> undoSelectionStack = new Stack<FocusedRange>();
  private final Stack<FocusedRange> redoSelectionStack = new Stack<FocusedRange>();


  private final UndoManagerPlus<BufferedDocOp> undoManager;
  private FocusedRange pendingCheckpoint;

  private static final LoggerBundle logger = new DomLogger("undo");

  private static final FocusedRange UNKNOWN_SELECTION = new FocusedRange(0, 0);


  private boolean bypass = false;

  public EditorUndoManagerImpl(UndoManagerPlus<BufferedDocOp> undoManager,
      SilentOperationSink<BufferedDocOp> sink, SelectionHelper selectionHelper) {
    Preconditions.checkNotNull(undoManager, "UndoManager must not be null");
    Preconditions.checkNotNull(sink, "Op sink must not be null");
    Preconditions.checkNotNull(selectionHelper, "Selection helper must not be null");
    this.sink = sink;
    this.undoManager = undoManager;
    this.selectionHelper = selectionHelper;
  }

  @Override
  public void undoableOp(BufferedDocOp op) {
    if (bypass) {
      return;
    }

    if (pendingCheckpoint != null) {
      if (logger.trace().shouldLog()) {
        logger.log(Level.TRACE, "checkpointing, selection known?"
            + (pendingCheckpoint != UNKNOWN_SELECTION) + " undo selection stack size: "
            + undoSelectionStack.size());
      }
      if (pendingCheckpoint == UNKNOWN_SELECTION) {
        pendingCheckpoint = selectionHelper.getSelectionRange();
        if (pendingCheckpoint == null) {
          pendingCheckpoint = UNKNOWN_SELECTION;
        }
      }

      undoManager.checkpoint();
      undoSelectionStack.push(pendingCheckpoint);
      pendingCheckpoint = null;
    }
    undoManager.undoableOp(op);
    if (redoSelectionStack.size() != 0) {
      if (logger.trace().shouldLog()) {
        logger.log(Level.TRACE, "redoStack cleared " + redoSelectionStack.size());
      }
      redoSelectionStack.clear();
    }
  }

  @Override
  public void nonUndoableOp(BufferedDocOp op) {
    if (bypass) {
      return;
    }

    undoManager.nonUndoableOp(op);
  }


  @Override
  public void maybeCheckpoint() {
    if (pendingCheckpoint == null) {
      pendingCheckpoint = UNKNOWN_SELECTION;
    }
  }

  @Override
  public void maybeCheckpoint(int startLocation, int endLocation) {
    pendingCheckpoint = new FocusedRange(startLocation, endLocation);
  }

  @Override
  public void undo() {
    Pair<BufferedDocOp, BufferedDocOp> pair = undoManager.undoPlus();

    if (pair == null || pair.first == null)  {
      if (logger.trace().shouldLog()) {
        logger.log(Level.TRACE, "cannot undo " + undoSelectionStack.size());
      }
      return;
    }

    BufferedDocOp undo = pair.first;
    BufferedDocOp transformedNonUndoable = pair.second;

    {
      FocusedRange selection = selectionHelper.getSelectionRange();

      if (selection != null) {
        redoSelectionStack.push(selection);
      } else {
        redoSelectionStack.push(UNKNOWN_SELECTION);
      }
    }
    bypassUndoStack(undo);
    restoreSelectionAfterUndoRedo(transformedNonUndoable, undoSelectionStack);

    logger.trace().log("Undoing!");
  }

  @Override
  public void redo() {
    Pair<BufferedDocOp, BufferedDocOp> pair = undoManager.redoPlus();

    if (pair == null || pair.first == null) {
      if (logger.trace().shouldLog()) {
        logger.log(Level.TRACE, "cannot redo " + redoSelectionStack.size());
      }
      return;
    }

    BufferedDocOp redo = pair.first;
    BufferedDocOp transformedNonUndoable = pair.second;

    {
      FocusedRange selection = selectionHelper.getSelectionRange();

      if (selection != null) {
        undoSelectionStack.push(selection);
      } else {
        undoSelectionStack.push(UNKNOWN_SELECTION);
      }
    }

    bypassUndoStack(redo);
    restoreSelectionAfterUndoRedo(transformedNonUndoable, redoSelectionStack);

    logger.trace().log("Redoing!");
  }

  /**
   * Applies an op locally and send it bypassing the undo stack. This is
   * neccessary with operations popped from the undoManager as they are
   * automatically applied.
   *
   * @param op
   */
  private void bypassUndoStack(BufferedDocOp op) {
    bypass = true;
    try {
      sink.consume(op);
    } finally {
      bypass = false;
    }
  }

  private FocusedRange restoreSelectionAfterUndoRedo(BufferedDocOp transformedNonUndoable,
      Stack<FocusedRange> selectionStack) {
    if (selectionStack.isEmpty()) {
      logger.log(Level.ERROR,
          "SelectionStack empty! This probably shouldn't be reached, but we can live with it.");
      return null;
    }
    FocusedRange selection = selectionStack.pop();
    if (selection == UNKNOWN_SELECTION) {
      logger.log(Level.TRACE, "unknown selection");
      return null;
    } else {
      if (transformedNonUndoable != null) {
        selection =
            RangeHelper.applyModifier(transformedNonUndoable, selection);
      }
      selectionHelper.setSelectionRange(selection);
      return selection;
    }
  }
}