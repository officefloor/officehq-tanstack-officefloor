package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class AddPoolMember {
    public void add(@Val PoolMember member, Pools pools) {
        pools.addMember(member);
    }
}
