package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_HELD or PERSON_RELEASED once the person is saved; the signed-in user arrives as X-User. */
public class AuditPersonHold {
    public void audit(@Val Person person, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.getSheetId()
                + " by=" + (user == null ? "" : user.trim()) + " name=" + person.getName());
    }
}
