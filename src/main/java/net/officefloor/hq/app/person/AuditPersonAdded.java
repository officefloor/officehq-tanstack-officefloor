package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_ADDED once the person is saved; the signed-in user arrives as X-User. */
public class AuditPersonAdded {
    public void audit(@Val Person person, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record(PersonAudit.line("PERSON_ADDED", person, user));
    }
}
