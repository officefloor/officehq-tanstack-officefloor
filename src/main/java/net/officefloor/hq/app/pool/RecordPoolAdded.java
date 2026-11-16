package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes POOL_ADDED to the audit file once the pool is stored. */
public class RecordPoolAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val PoolValues values, Audit audit) {
        audit.record("POOL_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " name=" + values.name());
    }
}
