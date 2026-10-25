package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordStatementCancelled {
    public void record(@Val Person person, @Val CancelledStatement cancelled, @Val ChangedBy by, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + person.getSheetId() + " by=" + by.name() + " name="
                + person.getName() + " due-total="
                + (cancelled.dueTotal() == null ? "n/a" : cancelled.dueTotal().toPlainString()));
    }
}
