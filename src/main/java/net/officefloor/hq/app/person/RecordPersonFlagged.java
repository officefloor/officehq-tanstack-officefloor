package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the flag just given (PERSON_FLAGGED) with its reason and until-date. */
public class RecordPersonFlagged {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " reason=" + person.getFlagReason()
                + " until=" + person.getFlagUntil());
    }
}
