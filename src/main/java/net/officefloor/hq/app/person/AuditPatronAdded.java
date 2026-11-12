package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a patron added: PATRON_ADDED sheet=<id> by=<user> name=<name> patron=<patron name>. */
public class AuditPatronAdded {
    public void audit(@Val PatronRequest request, @Val Person person, @Val Patron patron, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " patron=" + patron.person().getName());
    }
}
