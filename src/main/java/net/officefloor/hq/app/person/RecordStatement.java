package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a statement that went through is recorded. */
public class RecordStatement {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " due-total=" + person.getStatedTotal()
                + " claims=" + person.getStatedClaims()
                + (person.getStatedAsOf() == null ? "" : " asof=" + person.getStatedAsOf()));
    }
}
