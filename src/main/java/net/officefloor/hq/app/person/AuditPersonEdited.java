package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a stored person edit: PERSON_EDITED sheet=<id> by=<user> name=<name> grade=<grade>. */
public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person person, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " grade=" + person.getGrade());
    }
}
