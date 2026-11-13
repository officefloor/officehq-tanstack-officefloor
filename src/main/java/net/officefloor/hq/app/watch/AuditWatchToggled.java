package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the user starting or stopping watching the person. */
public class AuditWatchToggled {
    public void audit(@Val Person person, @Val Watching watching, @RequestParam("user") String user, Audit audit) {
        audit.record((watching.value() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.sheetId()
                + " by=" + user + " name=" + person.name());
    }
}
