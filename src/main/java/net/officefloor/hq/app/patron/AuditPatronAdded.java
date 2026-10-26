package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPatronAdded {
    public void audit(@Val Person person, @Val Patron patron, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PATRON_ADDED", person.sheetId(), Audit.user(connection),
                "name=" + person.name(), "patron=" + patron.person().name()));
    }
}
