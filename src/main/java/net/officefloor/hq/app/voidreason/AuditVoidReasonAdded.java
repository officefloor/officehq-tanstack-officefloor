package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditVoidReasonAdded {
    public void audit(@Val VoidReason reason, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("VOID_REASON_ADDED", reason.sheetId(), Audit.user(connection), "reason=" + reason.reason()));
    }
}
