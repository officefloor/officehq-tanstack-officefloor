package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Val;

/** Record that the merge was undone, then how the sheet's figures moved over it. */
public class AuditPersonMergeUndone {
    public void audit(@Val MergeUndoRequest request, @Val MergeUndone undone, @Val Rollup before, Audit audit,
            Rollups rollups) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + undone.sheetId() + " by=" + request.user() + " name="
                + undone.name() + " from=" + undone.from());
        rollups.record(undone.sheetId(), request.user(), before);
    }
}
