package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_MERGE_UNDONE name=<person restored> from=<person merged into> to the audit file once the undo is
 *  stored, then the figures it moved (see Rollup). */
public class RecordMergeUndone {
    public void record(@Val PersonMergeUndo undo, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String sheetId = undo.into().sheetId();
        audit.record("PERSON_MERGE_UNDONE sheet=" + sheetId + " by=" + undo.by() + " name=" + undo.name() + " from="
                + undo.into().name());
        rollup.record(sheetId, undo.by(), before);
    }
}
