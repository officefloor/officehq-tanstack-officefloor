package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPoolMember {
    public void respond(@Val PoolMember member, ObjectResponse<PoolResponse.Member> response) {
        response.send(new PoolResponse.Member(member.isPool() ? null : member.personId(), member.name(), member.share().toPlainString(),
                false, "", member.memberPoolId()));
    }
}
