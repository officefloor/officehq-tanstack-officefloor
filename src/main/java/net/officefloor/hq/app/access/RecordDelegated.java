package net.officefloor.hq.app.access;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordDelegated {
    public void record(@Val Delegation delegation, @Val ChangedBy by, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.sheetId() + " by=" + by.name() + " user=" + delegation.user()
                + " from=" + delegation.from() + " to=" + delegation.to());
    }
}
