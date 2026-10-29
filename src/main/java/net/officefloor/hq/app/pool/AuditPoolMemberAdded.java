package net.officefloor.hq.app.pool;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPoolMemberAdded {
    // The from-date and the to-date end the line, each only when given.
    public void audit(@Val PoolMember member, ServerHttpConnection connection, Audit audit) {
        List<String> fields = new ArrayList<>(List.of("pool=" + member.pool(), "name=" + member.name(),
                "share=" + member.share().toPlainString()));
        if (member.from() != null) {
            fields.add("from=" + member.from());
        }
        if (member.to() != null) {
            fields.add("to=" + member.to());
        }
        audit.record(Audit.line("POOL_MEMBER_ADDED", member.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
