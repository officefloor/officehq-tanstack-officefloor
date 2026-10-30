package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the person to the pool with their share. */
public class SavePoolMember {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPoolMember member, Pools pools, Audit audit,
            ObjectResponse<List<PoolResponse>> response) {
        pools.addMember(sheetId, member.pool(), member.personId(), member.share());
        audit.recordOnCommit("POOL_MEMBER_ADDED sheet=" + sheetId + " by=" + member.user() + " pool=" + member.pool()
                + " name=" + member.personName() + " share=" + member.share().toPlainString());
        response.send(pools.list(sheetId));
    }
}
