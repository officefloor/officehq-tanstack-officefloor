package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the hold just added to the person (their last), by its from-date. */
public class AuditPersonHeld {
    public void audit(@Val Person person, @RequestParam("user") String user, Audit audit) {
        Hold hold = person.holds().get(person.holds().size() - 1);
        audit.record("PERSON_HELD sheet=" + person.sheetId() + " by=" + user + " name=" + person.name() + " from="
                + hold.from());
    }
}
