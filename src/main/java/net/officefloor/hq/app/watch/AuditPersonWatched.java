package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record a watch that changed in the audit file: PERSON_WATCHED or PERSON_UNWATCHED. */
public class AuditPersonWatched {
    public void audit(@Val WatchInput input, @Val Person loaded, @Val WatchResponse saved, Audit audit) {
        if (!saved.changed()) {
            return;
        }
        audit.record((saved.watched() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + loaded.sheetId()
                + " by=" + input.user() + " name=" + loaded.name());
    }
}
