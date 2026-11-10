package net.officefloor.hq.app.pool;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the pool added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditPoolAdded {
    public void audit(@Val Pool pool, HttpServletRequest request, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.sheetId() + " by=" + Audit.user(request) + " name=" + pool.name());
    }
}
