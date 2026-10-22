package net.officefloor.hq.app.delegation;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored delegation in the audit file. */
public class AuditDelegated {
    public void audit(@Val Delegation d, Audit audit) {
        audit.record("DELEGATED sheet=" + d.sheetId() + " by=" + d.by() + " user=" + d.user()
                + " from=" + d.from() + " to=" + d.to());
    }
}
