package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the void reason added to the sheet. */
public class AuditVoidReasonAdded {
    public void audit(@Val VoidReason reason, @RequestParam("user") String user, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.sheetId() + " by=" + user + " reason=" + reason.reason());
    }
}
