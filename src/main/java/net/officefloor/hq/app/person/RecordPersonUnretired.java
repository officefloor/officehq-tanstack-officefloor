package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only an unretirement that went through is recorded, on the day it ended the retirement. */
public class RecordPersonUnretired {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        List<Retirement> past = person.getRetirements();
        audit.record("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " on=" + past.get(past.size() - 1).to());
    }
}
