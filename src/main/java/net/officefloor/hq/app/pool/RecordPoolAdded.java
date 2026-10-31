package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the pool is saved, so a refused one writes no audit line. */
public class RecordPoolAdded {
    public void record(@Val Pool pool, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.sheetId() + " by=" + pool.by() + " name=" + pool.name());
    }
}
