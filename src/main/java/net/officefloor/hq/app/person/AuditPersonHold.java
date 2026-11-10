package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the person put on hold, or released from it. Runs after it is saved. */
public class AuditPersonHold {
    public void audit(@Val Sheet sheet, @Val Person person,
            HttpServletRequest request, Audit audit) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + sheet.id()
                + " by=" + Audit.user(request) + " name=" + person.getName());
    }
}
