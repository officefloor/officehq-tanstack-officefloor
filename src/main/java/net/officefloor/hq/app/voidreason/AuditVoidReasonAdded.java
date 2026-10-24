package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the void reason, once it is added. */
public class AuditVoidReasonAdded {
    public void audit(@Val VoidReason reason, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.getSheetId() + " by=" + AuditUser.decode(user)
                + " reason=" + reason.getReason());
    }
}
