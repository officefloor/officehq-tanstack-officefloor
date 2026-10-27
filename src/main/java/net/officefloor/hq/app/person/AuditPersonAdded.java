package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonAdded {
    public void audit(@Val Person person, ServerHttpConnection connection, Audit audit) {
        // terms= only when the form gave the person terms of their own; contact= only when it gave a contact.
        List<String> fields = new ArrayList<>(List.of("name=" + person.name(), "grade=" + person.grade()));
        if (person.terms() != null) {
            fields.add("terms=" + person.terms());
        }
        if (person.contact() != null) {
            fields.add("contact=" + person.contact());
        }
        audit.record(Audit.line("PERSON_ADDED", person.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
