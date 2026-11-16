package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Writes STATEMENT to the audit file once the statement is stored. */
public class RecordStatementTaken {
    public void record(@Val Statement statement, @Val Person person, Audit audit) {
        audit.record("STATEMENT sheet=" + statement.sheetId() + " by=" + statement.by() + " name=" + person.name()
                + " due-total=" + statement.dueTotal() + " claims=" + statement.claims());
    }
}
