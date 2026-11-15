package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a delegation that went through is recorded. */
public class RecordDelegated {
    public void record(@Val Delegation delegation, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.sheetId() + " by=" + delegation.by()
                + " user=" + delegation.user() + " from=" + delegation.from() + " to=" + delegation.to());
    }
}
