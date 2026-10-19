package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the hold added to the person and its from-date, once it is saved. */
public class AuditPersonHold {
    public void audit(@Val Person person, @Val Hold hold, @RequestParam(value = "user", required = false) String user,
            Audit audit) {
        audit.record("PERSON_HELD sheet=" + person.getSheetId() + " by=" + user + " name=" + person.getName()
                + " from=" + hold.from());
    }
}
