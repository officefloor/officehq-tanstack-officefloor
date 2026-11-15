package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a pool that went through is recorded. */
public class RecordPoolAdded {
    public void record(@Val PoolAdded pool, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.sheetId() + " by=" + pool.by() + " name=" + pool.name());
    }
}
