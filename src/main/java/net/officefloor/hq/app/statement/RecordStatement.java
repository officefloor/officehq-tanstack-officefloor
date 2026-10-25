package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordStatement {
    public void record(@Val Person person, @Val Statement statement, @Val ChangedBy by, Audit audit) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() == null ? "n/a" : statement.dueTotal().toPlainString())
                + " claims=" + statement.claims() + (statement.asOf() == null ? "" : " asof=" + statement.asOf()));
    }
}
