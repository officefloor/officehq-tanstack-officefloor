package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a void reason that went through is recorded. */
public class RecordVoidReasonAdded {
    public void record(@Val VoidReason reason, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.sheetId() + " by=" + reason.by() + " reason=" + reason.reason());
    }
}
