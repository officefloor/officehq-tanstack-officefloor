package net.officefloor.hq.app.alias;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the alias the person was just given. */
public class AuditAliasAdded {
    public void audit(@Val Person person, @Val Alias alias, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " alias=" + alias.alias());
    }
}
