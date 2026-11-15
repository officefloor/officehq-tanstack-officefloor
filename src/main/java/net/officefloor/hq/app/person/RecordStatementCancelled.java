package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a cancellation that went through is recorded, with the cancelled statement's due-total. */
public class RecordStatementCancelled {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " due-total=" + person.getCancelledTotal());
    }
}
