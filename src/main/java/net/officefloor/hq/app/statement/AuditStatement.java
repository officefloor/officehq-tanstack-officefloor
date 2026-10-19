package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the statement once it is taken and saved. */
public class AuditStatement {
    public void audit(@Val Person person, @Val Statement statement,
            @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("STATEMENT sheet=" + person.getSheetId() + " by=" + user + " name=" + person.getName()
                + " due-total=" + statement.dueTotalText() + " claims=" + statement.claims());
    }
}
