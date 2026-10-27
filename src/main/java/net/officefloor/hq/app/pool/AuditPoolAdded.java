package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPoolAdded {
    public void audit(@Val Pool pool, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("POOL_ADDED", pool.sheetId(), Audit.user(connection), "name=" + pool.name()));
    }
}
