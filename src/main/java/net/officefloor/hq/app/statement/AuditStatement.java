package net.officefloor.hq.app.statement;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the statement taken. Runs after it is saved. */
public class AuditStatement {
    public void audit(@Val Sheet sheet, @Val Person person, @Val Statement statement,
            HttpServletRequest request, Audit audit) {
        audit.record("STATEMENT sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " due-total=" + statement.dueTotal()
                + " claims=" + statement.claims());
    }
}
