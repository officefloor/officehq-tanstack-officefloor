package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class SavePool {
    public void save(@Val PoolName pool, PoolRepository pools) {
        pools.add(pool.sheetId(), pool.name());
    }
}
