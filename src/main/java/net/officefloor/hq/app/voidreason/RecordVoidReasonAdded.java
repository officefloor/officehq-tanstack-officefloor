package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the void reason is saved, so a refused one writes no audit line. */
public class RecordVoidReasonAdded {
    public void record(@Val VoidReason reason, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.sheetId() + " by=" + reason.by() + " reason=" + reason.reason());
    }
}
