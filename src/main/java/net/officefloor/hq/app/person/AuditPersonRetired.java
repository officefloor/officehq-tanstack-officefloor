package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonRetired {
    public void audit(@Val Person loaded, @Val PersonResponse retired, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_RETIRED", loaded.sheetId(), Audit.user(connection), "name=" + retired.name(),
                "from=" + retired.retiredFrom()));
    }
}
