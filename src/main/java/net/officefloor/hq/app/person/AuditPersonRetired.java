package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_RETIRED once the person is saved; the signed-in user arrives as X-User. */
public class AuditPersonRetired {
    public void audit(@Val Person person, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " name=" + person.getName() + " from=" + person.getRetiredFrom());
    }
}
