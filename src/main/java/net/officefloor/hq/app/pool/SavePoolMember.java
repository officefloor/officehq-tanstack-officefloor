package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class SavePoolMember {
    public void save(@Val NewPoolMember added, PoolRepository pools) {
        pools.addMember(added.pool().id(), added.member());
    }
}
