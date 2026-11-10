package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the move: the person's new position, counting from 1. Runs after it is saved. */
public class AuditPersonMoved {
    public void audit(@Val Sheet sheet, @Val Person person, @Val Moved moved,
            HttpServletRequest request, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " position=" + moved.position());
    }
}
