package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** PERSON_WATCHED or PERSON_UNWATCHED, naming the person; nothing when the user already was, or was not, watching. */
public class RecordPersonWatch {
    public void record(@Val WatchInput watch, @Val WatchChanged changed, @Val Person person, @Val ChangedBy by,
            Audit audit) {
        if (changed.changed()) {
            audit.record((watch.on() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.getSheetId()
                    + " by=" + by.name() + " name=" + person.getName());
        }
    }
}
