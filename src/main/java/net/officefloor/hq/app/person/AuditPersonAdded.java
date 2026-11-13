package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just added; runs only once the add has passed every rule. */
public class AuditPersonAdded {
    public void audit(@Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " grade=" + person.grade());
    }
}
