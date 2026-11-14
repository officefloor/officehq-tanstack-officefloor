package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the hold just put on (PERSON_HELD) or taken off (PERSON_RELEASED). */
public class RecordPersonHold {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet="
                + person.getSheetId() + " by=" + (by == null ? "" : by) + " name=" + person.getName());
    }
}
