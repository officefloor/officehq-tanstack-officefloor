package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the retired person and the date they are retired from, once it is saved. */
public class AuditPersonRetired {
    public void audit(@Val Person person, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + user
                + " name=" + person.getName() + " from=" + person.getRetiredFrom());
    }
}
