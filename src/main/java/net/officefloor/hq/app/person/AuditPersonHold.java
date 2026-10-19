package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person put on hold, or taken off it, once it is saved. */
public class AuditPersonHold {
    public void audit(@Val Person person, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.getSheetId()
                + " by=" + user + " name=" + person.getName());
    }
}
