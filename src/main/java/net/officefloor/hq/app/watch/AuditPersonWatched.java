package net.officefloor.hq.app.watch;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the watch started or stopped. Runs after it is saved; a watch that changed nothing writes nothing. */
public class AuditPersonWatched {
    public void audit(@Val Sheet sheet, @Val Person person, @Val WatchChange change,
            HttpServletRequest request, Audit audit) {
        if (!change.changed()) return;
        audit.record((change.on() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + sheet.id()
                + " by=" + Audit.user(request) + " name=" + person.getName());
    }
}
