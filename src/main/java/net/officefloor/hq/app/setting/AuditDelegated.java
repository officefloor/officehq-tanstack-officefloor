package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

public class AuditDelegated {
    public void audit(@Val Delegation delegation, Audit audit) {
        audit.record(Audit.line("DELEGATED", delegation.sheetId(), delegation.by(), "user=" + delegation.user(),
                "from=" + delegation.from(), "to=" + delegation.to()));
    }
}
