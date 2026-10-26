package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditStatement {
    public void audit(@Val Person person, @Val Statement statement, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("STATEMENT", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                "due-total=" + statement.dueTotal(), "claims=" + statement.claims()));
    }
}
