package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the statement is saved. */
public class RecordStatement {
    public void record(@Val Statement statement, @Val Person person, Audit audit) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + statement.user()
                + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() != null ? statement.dueTotal().toPlainString() : "n/a")
                + " claims=" + statement.claims());
    }
}
