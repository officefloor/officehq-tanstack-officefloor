package net.officefloor.hq.app.voidreason;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the void reason added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditVoidReasonAdded {
    public void audit(@Val VoidReason reason, HttpServletRequest request, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + reason.sheetId() + " by=" + Audit.user(request)
                + " reason=" + reason.reason());
    }
}
