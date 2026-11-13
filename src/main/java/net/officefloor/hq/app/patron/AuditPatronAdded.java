package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the patron the person just named. */
public class AuditPatronAdded {
    public void audit(@Val Person person, @Val PatronName patron, @RequestParam("user") String user, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name() + " patron="
                + patron.name());
    }
}
