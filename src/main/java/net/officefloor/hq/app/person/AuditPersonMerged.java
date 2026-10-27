package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonMerged {
    public void audit(@Val MergedPerson merged, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_MERGED", merged.from().sheetId(), Audit.user(connection),
                "from=" + merged.from().name(), "into=" + merged.into().name()));
    }
}
