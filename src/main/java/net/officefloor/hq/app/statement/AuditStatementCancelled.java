package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the statement cancelled in the audit file, with its due-total as the screen showed it (n/a when none). */
public class AuditStatementCancelled {
    public void audit(@Val StatementRequest request, @Val Statement statement, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + statement.sheetId() + " by=" + request.user()
                + " name=" + statement.name()
                + " due-total=" + (statement.dueTotal() != null ? statement.dueTotal().toPlainString() : "n/a"));
    }
}
