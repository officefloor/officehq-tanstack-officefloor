package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the statement taken in the audit file, with the due-total as the screen shows it (n/a when none),
 *  and the date it was taken as of when one was given. */
public class AuditStatement {
    public void audit(@Val StatementRequest request, @Val Statement statement, Audit audit) {
        audit.record("STATEMENT sheet=" + statement.sheetId() + " by=" + request.user()
                + " name=" + statement.name()
                + " due-total=" + (statement.dueTotal() != null ? statement.dueTotal().toPlainString() : "n/a")
                + " claims=" + statement.claims()
                + (statement.asOf() != null ? " asof=" + statement.asOf() : ""));
    }
}
