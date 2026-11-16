package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes DELEGATED to the audit file once the delegation is stored. */
public class RecordDelegated {
    public void record(@Val Delegation delegation, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.sheetId() + " by=" + delegation.steward() + " user="
                + delegation.user() + " from=" + delegation.from() + " to=" + delegation.to());
    }
}
