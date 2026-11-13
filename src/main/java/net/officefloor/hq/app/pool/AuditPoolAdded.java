package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the pool added to the sheet. */
public class AuditPoolAdded {
    public void audit(@Val PoolName pool, @RequestParam("user") String user, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.sheetId() + " by=" + user + " name=" + pool.name());
    }
}
