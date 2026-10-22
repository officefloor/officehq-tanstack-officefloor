package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored add in the audit file. */
public class AuditPersonAdded {
    public void audit(@Val PersonRequest request, @Val Person person, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.sheetId() + " by=" + request.user()
                + " name=" + person.name() + " grade=" + person.grade());
    }
}
