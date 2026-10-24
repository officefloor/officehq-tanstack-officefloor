package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the delegation, once it is saved. */
public class AuditDelegated {
    public void audit(@Val Delegation delegation, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.getSheetId() + " by=" + delegation.getByName()
                + " user=" + delegation.getUserName() + " from=" + delegation.getFromDate() + " to=" + delegation.getToDate());
    }
}
