package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records the user starting or stopping watching the person, once it is saved, when it changed anything. */
public class AuditWatch {
    public void audit(@Val WatchInput input, @Val WatchChange change, @Val Person person, Audit audit) {
        if (!change.changed()) {
            return;
        }
        audit.record((input.on() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.getSheetId()
                + " by=" + input.user() + " name=" + person.getName());
    }
}
