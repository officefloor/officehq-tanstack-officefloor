package net.officefloor.hq.app.statement;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the statement cancelled. Runs after it is saved. */
public class AuditStatementCancelled {
    public void audit(@Val Sheet sheet, @Val Person person, @Val CancelledStatement cancelled,
            HttpServletRequest request, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " due-total=" + cancelled.dueTotal());
    }
}
