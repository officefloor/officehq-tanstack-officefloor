package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_ALIAS_ADDED to the audit file once the alias is stored. */
public class RecordAliasAdded {
    public void record(@Val Person person, @Val AliasValues values, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " alias=" + values.alias());
    }
}
