package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPoolAdded {
    public void record(@Val Pool pool, @Val ChangedBy by, Audit audit) {
        audit.record("POOL_ADDED sheet=" + pool.sheetId() + " by=" + by.name() + " name=" + pool.name());
    }
}
