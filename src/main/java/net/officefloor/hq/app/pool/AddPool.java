package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class AddPool {
    public void add(@Val Pool pool, Pools pools) {
        pools.add(pool);
    }
}
