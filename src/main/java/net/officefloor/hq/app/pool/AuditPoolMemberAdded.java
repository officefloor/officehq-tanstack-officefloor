package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPoolMemberAdded {
    public void audit(@Val PoolMember member, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("POOL_MEMBER_ADDED", member.sheetId(), Audit.user(connection), "pool=" + member.pool(),
                "name=" + member.name(), "share=" + member.share().toPlainString()));
    }
}
