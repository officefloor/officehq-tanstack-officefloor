package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordAliasAdded {
    public void record(@Val Person person, @Val AliasInput added, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " alias=" + added.alias());
    }
}
