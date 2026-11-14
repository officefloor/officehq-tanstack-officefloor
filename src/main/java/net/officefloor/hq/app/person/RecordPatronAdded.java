package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the patron just named (PATRON_ADDED). */
public class RecordPatronAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, @Val Patron patron, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " patron=" + patron.person().getName());
    }
}
