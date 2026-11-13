package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just put on hold, or just taken off it. */
public class AuditPersonHeld {
    public void audit(@Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record((person.held() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.sheetId() + " by="
                + user + " name=" + person.name());
    }
}
