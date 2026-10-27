package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPoolMember {
    public void respond(@Val PoolMember member, ObjectResponse<PoolMember> response) {
        response.send(member);
    }
}
