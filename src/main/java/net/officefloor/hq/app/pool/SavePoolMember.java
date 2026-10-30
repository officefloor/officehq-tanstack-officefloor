package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the person, or the other pool, to the pool with their share. */
public class SavePoolMember {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPoolMember member, Pools pools, Audit audit,
            ObjectResponse<List<PoolResponse>> response) {
        pools.addMember(sheetId, member.pool(), member.personId(), member.memberPool(), member.share(),
                member.from(), member.to());
        audit.recordOnCommit("POOL_MEMBER_ADDED sheet=" + sheetId + " by=" + member.user() + " pool=" + member.pool()
                + " name=" + member.memberName() + " share=" + member.share().toPlainString()
                + (member.from() == null ? "" : " from=" + member.from())
                + (member.to() == null ? "" : " to=" + member.to()));
        response.send(pools.list(sheetId));
    }
}
