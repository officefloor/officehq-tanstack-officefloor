package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just retired and the date they are retired from. */
public class AuditPersonRetired {
    public void audit(@Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " from=" + person.retiredFrom());
    }
}
