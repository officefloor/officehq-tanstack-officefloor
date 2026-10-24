package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the pool member, once they are added. */
public class AuditPoolMemberAdded {
    public void audit(@Val Pool pool, @Val ApplyPoolMember.MemberName name, @Val PoolMember member,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + pool.getSheetId() + " by=" + AuditUser.decode(user)
                + " pool=" + pool.getName() + " name=" + name.name() + " share=" + member.getShare().toPlainString()
                + (member.getFromDate() == null ? "" : " from=" + member.getFromDate())
                + (member.getToDate() == null ? "" : " to=" + member.getToDate()));
    }
}
