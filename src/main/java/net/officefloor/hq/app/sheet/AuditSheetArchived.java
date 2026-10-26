package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSheetArchived {
    public void audit(@Val Sheet sheet, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("SHEET_ARCHIVED", sheet.id(), Audit.user(connection)));
    }
}
