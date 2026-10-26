package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonAdded {
    public void audit(@Val Person person, ServerHttpConnection connection, Audit audit) {
        // terms= only when the form gave the person terms of their own.
        if (person.terms() == null) {
            audit.record(Audit.line("PERSON_ADDED", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                    "grade=" + person.grade()));
        } else {
            audit.record(Audit.line("PERSON_ADDED", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                    "grade=" + person.grade(), "terms=" + person.terms()));
        }
    }
}
