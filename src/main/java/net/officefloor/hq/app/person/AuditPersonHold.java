package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the hold put on the person. Runs after it is saved. */
public class AuditPersonHold {
    public void audit(@Val Sheet sheet, @Val Person person,
            HttpServletRequest request, Audit audit) {
        List<Hold> holds = person.getHolds();
        audit.record("PERSON_HELD sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " from=" + holds.get(holds.size() - 1).from());
    }
}
