package net.officefloor.hq.app.pool;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the member added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditPoolMemberAdded {
    public void audit(@Val PoolMember member, HttpServletRequest request, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + Audit.user(request)
                + " pool=" + member.pool() + " name=" + member.name() + " share=" + member.share().toPlainString());
    }
}
