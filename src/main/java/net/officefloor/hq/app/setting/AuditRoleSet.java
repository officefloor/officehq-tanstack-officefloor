package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditRoleSet {
    public void audit(@Val Role role, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("ROLE_SET", role.sheetId(), Audit.user(connection),
                "user=" + role.user(), "role=" + role.role()));
    }
}
