package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the patron named, once it is saved. */
public class AuditPatronAdded {
    public void audit(@Val Person person, @Val PatronAdded added,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " patron=" + added.patron().getName());
    }
}
