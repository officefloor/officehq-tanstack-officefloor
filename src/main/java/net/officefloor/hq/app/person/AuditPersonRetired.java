package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a retirement: PERSON_RETIRED sheet=<id> by=<user> name=<name> from=<date>. */
public class AuditPersonRetired {
    public void audit(@Val RetireInput input, @Val Person person, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " from=" + person.getRetiredFrom());
    }
}
