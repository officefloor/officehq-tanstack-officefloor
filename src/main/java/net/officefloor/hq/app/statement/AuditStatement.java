package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a statement: STATEMENT sheet=<id> by=<user> name=<name> due-total=<0.00 or n/a>
 *  claims=<count of their claims that counted>. */
public class AuditStatement {
    public void audit(@Val StatementRequest request, @Val Person person, @Val Statement statement, Audit audit) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + request.user() + " name=" + person.getName()
                + " due-total=" + (statement.dueTotal() == null ? "n/a" : statement.dueTotal().setScale(2).toPlainString())
                + " claims=" + statement.claims());
    }
}
