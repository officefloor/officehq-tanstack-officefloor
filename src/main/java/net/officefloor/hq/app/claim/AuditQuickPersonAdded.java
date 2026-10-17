package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAudit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_ADDED, as the person form does, when the quick-add bar created the person. */
public class AuditQuickPersonAdded {
    public void audit(@Val QuickPerson quick, @Val Person person, @HttpHeaderParameter("X-User") String user,
            Audit audit) {
        if (quick.added()) {
            audit.record(PersonAudit.line("PERSON_ADDED", person, user));
        }
    }
}
