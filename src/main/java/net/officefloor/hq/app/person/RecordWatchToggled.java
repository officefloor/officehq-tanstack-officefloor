package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the watch is saved, so a refused one writes no audit line. */
public class RecordWatchToggled {
    public void record(@Val WatchChange change, @Val Person person, Audit audit) {
        audit.record((change.watching() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.getSheetId()
                + " by=" + change.user() + " name=" + person.getName());
    }
}
