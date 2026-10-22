package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored alias in the audit file. */
public class AuditPersonAliasAdded {
    public void audit(@Val AliasInput input, @Val Person loaded, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + loaded.sheetId() + " by=" + input.user() + " name=" + loaded.name()
                + " alias=" + input.alias());
    }
}
