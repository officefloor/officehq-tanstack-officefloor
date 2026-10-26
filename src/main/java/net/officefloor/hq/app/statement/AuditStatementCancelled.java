package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditStatementCancelled {
    public void audit(@Val Person person, @Val CancelledStatement cancelled, ServerHttpConnection connection,
            Audit audit) {
        audit.record(Audit.line("STATEMENT_CANCELLED", person.sheetId(), Audit.user(connection),
                "name=" + person.name(), "due-total=" + cancelled.dueTotal()));
    }
}
