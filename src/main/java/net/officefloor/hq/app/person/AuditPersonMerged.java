package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the merge in the audit file. */
public class AuditPersonMerged {
    public void audit(@Val MergeInput input, @Val Merge merge, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + merge.sheetId() + " by=" + input.user() + " from=" + merge.from()
                + " into=" + merge.into());
    }
}
