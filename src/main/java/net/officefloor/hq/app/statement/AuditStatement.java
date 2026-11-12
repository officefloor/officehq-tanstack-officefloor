package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a statement: STATEMENT sheet=<id> by=<user> name=<name> due-total=<0.00 or n/a>
 *  claims=<count of their claims that counted>, then asof=<date> when it was taken as of a date. */
public class AuditStatement {
    public void audit(@Val StatementRequest request, @Val Person person, @Val Statement statement, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + request.user() + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() == null ? "n/a" : statement.dueTotal().setScale(2).toPlainString())
                + " claims=" + statement.claims()
                + (request.asOfDate() == null ? "" : " asof=" + request.asOfDate()));
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
