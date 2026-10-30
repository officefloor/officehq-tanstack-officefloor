package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AuditStatement {
    public void audit(@Val StatementRequest request, @Val Person person, @Val Statement statement, Audit audit, @Val RollupFigures before, Rollup rollup) {
        audit.recordOnCommit("STATEMENT sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() == null ? "n/a" : statement.dueTotal().toPlainString())
                + " claims=" + statement.claims());
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
