package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_WATCHED or PERSON_UNWATCHED to the audit file once the watch is stored. */
public class RecordWatchChanged {
    public void record(@Val Person person, @Val WatchChange change, Audit audit) {
        audit.record((change.on() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.sheetId() + " by="
                + change.by() + " name=" + person.name());
    }
}
