package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the flag. Runs after it is saved. */
public class AuditPersonFlagged {
    public void audit(@Val Sheet sheet, @Val Person person,
            HttpServletRequest request, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " reason=" + person.getFlagReason() + " until=" + person.getFlagUntil());
    }
}
