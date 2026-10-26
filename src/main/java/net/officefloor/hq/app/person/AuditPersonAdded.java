package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonAdded {
    public void audit(@Val Person person, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_ADDED", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                "grade=" + person.grade()));
    }
}
