package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the alias added, once it is saved. */
public class AuditAliasAdded {
    public void audit(@Val Person person, @Val AliasInput input,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " alias=" + input.alias());
    }
}
