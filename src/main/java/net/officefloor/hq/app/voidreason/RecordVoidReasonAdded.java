package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordVoidReasonAdded {
    public void record(@Val VoidReason reason, @Val ChangedBy by, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.sheetId() + " by=" + by.name() + " reason=" + reason.reason());
    }
}
