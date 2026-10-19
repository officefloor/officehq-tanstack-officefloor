package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the patron named for the person, once it is saved. */
public class AuditPatronAdded {
    public void audit(@Val Person person, @Val Patron patron,
            @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + user
                + " name=" + person.getName() + " patron=" + patron.name());
    }
}
