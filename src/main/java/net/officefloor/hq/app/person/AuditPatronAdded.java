package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the patron named. Runs after it is saved. */
public class AuditPatronAdded {
    public void audit(@Val Sheet sheet, @Val Person person, @Val Patron patron,
            HttpServletRequest request, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " patron=" + patron.person().getName());
    }
}
