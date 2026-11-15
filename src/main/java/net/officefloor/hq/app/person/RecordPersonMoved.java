package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a move that went through is recorded. */
public class RecordPersonMoved {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, @Val PersonMoved moved, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " position=" + moved.position());
    }
}
