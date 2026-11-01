package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the undo is saved, so a refused one writes no audit line. */
public class RecordMergeUndone {
    public void record(@Val MergeUndone undone, Audit audit) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + undone.restored().getSheetId() + " by=" + undone.user()
                + " name=" + undone.restored().getName() + " from=" + undone.from().getName());
    }
}
