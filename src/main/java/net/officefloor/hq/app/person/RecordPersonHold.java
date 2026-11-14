package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the hold just added (PERSON_HELD) with its from-date. */
public class RecordPersonHold {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        Hold hold = person.getHolds().get(person.getHolds().size() - 1);
        audit.record("PERSON_HELD sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " from=" + hold.from());
    }
}
