package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the statement just cancelled: the person's name and the due-total it had recorded. */
public class AuditStatementCancelled {
    public void audit(@Val CancelledStatement statement, @Val Person person, @RequestParam("user") String user,
            Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + person.sheetId() + " by=" + user + " name=" + statement.name()
                + " due-total=" + statement.dueTotal());
    }
}
