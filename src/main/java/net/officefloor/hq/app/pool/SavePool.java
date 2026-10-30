package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the pool to the sheet. */
public class SavePool {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPool pool, Pools pools, Audit audit,
            ObjectResponse<List<PoolResponse>> response) {
        pools.add(sheetId, pool.name());
        audit.recordOnCommit("POOL_ADDED sheet=" + sheetId + " by=" + pool.user() + " name=" + pool.name());
        response.send(pools.list(sheetId));
    }
}
