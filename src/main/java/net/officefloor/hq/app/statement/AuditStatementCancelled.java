package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditStatementCancelled {
    public void audit(@Val StatementRequest request, @Val Person person, @Val CancelledStatement cancelled, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        audit.recordOnCommit("STATEMENT_CANCELLED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName()
                + " due-total=" + (cancelled.dueTotal() == null ? "n/a" : cancelled.dueTotal().setScale(2).toPlainString()));
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
