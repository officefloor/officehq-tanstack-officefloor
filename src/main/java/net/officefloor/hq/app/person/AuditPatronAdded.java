package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PATRON_ADDED once the person is saved; the signed-in user arrives as X-User. */
public class AuditPatronAdded {
    public void audit(@Val Person person, @Val AddedPatron patron, @HttpHeaderParameter("X-User") String user,
            Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " name=" + person.getName() + " patron=" + patron.name());
    }
}
