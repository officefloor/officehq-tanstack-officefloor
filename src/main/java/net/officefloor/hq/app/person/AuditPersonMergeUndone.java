package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a merge undo: PERSON_MERGE_UNDONE sheet=<id> by=<user> name=<the person restored> from=<the person
 *  merged into>, followed by how it moved the figures. */
public class AuditPersonMergeUndone {
    public void audit(@Val MergeUndoRequest request, @Val Person person, @Val MergeUndone undone,
            @Val Rollup before, Audit audit, RollupService rollups) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + undone.person().getName() + " from=" + person.getName());
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
