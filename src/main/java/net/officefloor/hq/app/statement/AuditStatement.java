package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the statement just taken: the person's name, their due-total and how many claims counted. */
public class AuditStatement {
    public void audit(@Val Statement statement, @Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("STATEMENT sheet=" + person.sheetId() + " by=" + user + " name=" + statement.name()
                + " due-total=" + statement.dueTotal() + " claims=" + statement.claims());
    }
}
