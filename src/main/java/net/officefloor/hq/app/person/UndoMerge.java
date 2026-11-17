package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Stores the undo of the merge (see MergeUndoer#undo). */
public class UndoMerge {
    public void undo(@Val PersonMergeUndo undo, MergeUndoer undoer) {
        undoer.undo(undo.into().sheetId(), undo.into().id(), undo.merge());
    }
}
