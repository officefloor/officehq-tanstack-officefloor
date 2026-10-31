package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;

public class SavePoolMember {
    public void save(@Val PoolMember member, Pools pools) {
        pools.addMember(member.sheetId(), member.pool(), member.personId(), member.share());
    }
}
