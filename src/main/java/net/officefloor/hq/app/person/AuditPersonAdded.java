package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the added person, once it is saved, with their terms when they have any, and their contact when they have one. */
public class AuditPersonAdded {
    public void audit(@Val Person person, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record(PersonRules.addedLine(person, AuditUser.decode(user)));
    }
}
