package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Undoes the person's last merge (see {@link MergeRepository#undo}). */
public class UndoMerge {
    public void undo(@Val MergeMade made, @Val Person person, MergeRepository merges) {
        merges.undo(made, person);
    }
}
