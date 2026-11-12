package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a merge: PERSON_MERGED sheet=<id> by=<user> from=<name> into=<name>, followed by how it moved the figures. */
public class AuditPersonMerged {
    public void audit(@Val MergeRequest request, @Val Person person, @Val MergedInto merged, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("PERSON_MERGED sheet=" + person.getSheetId() + " by=" + request.user() + " from="
                + person.getName() + " into=" + merged.person().getName());
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
