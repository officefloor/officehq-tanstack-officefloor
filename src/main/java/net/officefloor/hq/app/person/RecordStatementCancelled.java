package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the statement is cancelled: the cancelled statement's due-total. */
public class RecordStatementCancelled {
    public void record(@Val Statement statement, @Val Person person, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + person.getSheetId() + " by=" + statement.user()
                + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() != null ? statement.dueTotal().toPlainString() : "n/a"));
    }
}
