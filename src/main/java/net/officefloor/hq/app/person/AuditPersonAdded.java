package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a stored person add: PERSON_ADDED sheet=<id> by=<user> name=<name> grade=<grade>. */
public class AuditPersonAdded {
    public void audit(@Val PersonRequest request, @Val Person person, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " grade=" + person.getGrade());
    }
}
