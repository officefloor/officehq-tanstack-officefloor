package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record the stored patron in the audit file. */
public class AuditPatronAdded {
    public void audit(@Val PatronRequest request, @Val Person loaded, @Val Patron patron, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + patron.sheetId() + " by=" + request.user() + " name=" + loaded.name()
                + " patron=" + patron.patronName());
    }
}
