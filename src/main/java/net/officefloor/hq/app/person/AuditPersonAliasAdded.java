package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records an alias: PERSON_ALIAS_ADDED sheet=<id> by=<user> name=<name> alias=<alias>. */
public class AuditPersonAliasAdded {
    public void audit(@Val AliasRequest request, @Val Person person, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " alias=" + request.alias());
    }
}
