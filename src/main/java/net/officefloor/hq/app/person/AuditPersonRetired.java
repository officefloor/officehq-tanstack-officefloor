package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the retirement, once it is saved. */
public class AuditPersonRetired {
    public void audit(@Val Person person, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " from=" + person.getRetiredFrom());
    }
}
