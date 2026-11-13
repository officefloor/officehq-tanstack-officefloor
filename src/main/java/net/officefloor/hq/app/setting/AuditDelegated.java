package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the delegation just made; runs only once it has passed every rule. */
public class AuditDelegated {
    public void audit(@Val Delegation delegation, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.sheetId() + " by=" + delegation.by() + " user=" + delegation.user()
                + " from=" + delegation.from() + " to=" + delegation.to());
    }
}
