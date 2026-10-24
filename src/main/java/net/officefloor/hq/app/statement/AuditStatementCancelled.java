package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the cancelled statement, once it is removed. */
public class AuditStatementCancelled {
    public void audit(@Val Statement statement, @Val Person person,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + statement.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " due-total=" + statement.getDueTotal());
    }
}
