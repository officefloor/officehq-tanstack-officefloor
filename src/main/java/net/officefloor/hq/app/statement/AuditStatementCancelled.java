package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a cancelled statement: STATEMENT_CANCELLED sheet=<id> by=<user> name=<name>
 *  due-total=<the cancelled statement's due-total, 0.00 or n/a>. */
public class AuditStatementCancelled {
    public void audit(@Val StatementRequest request, @Val Person person, @Val Statement statement, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("STATEMENT_CANCELLED sheet=" + person.getSheetId() + " by=" + request.user().trim() + " name="
                + person.getName() + " due-total="
                + (statement.dueTotal() == null ? "n/a" : statement.dueTotal().setScale(2).toPlainString()));
        rollups.record(person.getSheetId(), request.user().trim(), before);
    }
}
