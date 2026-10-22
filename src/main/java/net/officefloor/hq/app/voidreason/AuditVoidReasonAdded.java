package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored void reason in the audit file. */
public class AuditVoidReasonAdded {
    public void audit(@Val VoidReason r, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + r.sheetId() + " by=" + r.by() + " reason=" + r.reason());
    }
}
