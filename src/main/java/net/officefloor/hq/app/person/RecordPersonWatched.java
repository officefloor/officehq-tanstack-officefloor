package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: PERSON_WATCHED or PERSON_UNWATCHED, only when the watch changed. */
public class RecordPersonWatched {
    public void record(@RequestParam(name = "by", required = false) String by, @Val Watch watch,
            @Val Person person, Audit audit) {
        if (!watch.changed()) {
            return;
        }
        audit.record((watch.watched() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.getSheetId()
                + " by=" + (by == null ? "" : by) + " name=" + person.getName());
    }
}
