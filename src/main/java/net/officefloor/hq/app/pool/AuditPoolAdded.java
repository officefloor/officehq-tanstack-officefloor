package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the pool, once it is added. */
public class AuditPoolAdded {
    public void audit(@Val Pool pool, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.getSheetId() + " by=" + AuditUser.decode(user) + " name=" + pool.getName());
    }
}
