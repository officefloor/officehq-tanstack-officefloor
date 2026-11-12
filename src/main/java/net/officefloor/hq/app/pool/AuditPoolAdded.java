package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a pool added: POOL_ADDED sheet=<id> by=<user> name=<name>. */
public class AuditPoolAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val PoolInput input, Audit audit) {
        audit.record("POOL_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " name=" + input.name());
    }
}
