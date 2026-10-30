package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonMergeUndone {
    public void audit(@Val MergeUndone undone, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_MERGE_UNDONE", undone.from().sheetId(), Audit.user(connection),
                "name=" + undone.restored().name(), "from=" + undone.from().name()));
    }
}
