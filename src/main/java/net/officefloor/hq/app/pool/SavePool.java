package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class SavePool {
    public void save(@Val Pool pool, Pools pools) {
        pools.add(pool.sheetId(), pool.name());
    }
}
