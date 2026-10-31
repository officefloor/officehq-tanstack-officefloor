package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the alias is saved. */
public class RecordAliasAdded {
    public void record(@Val Aliasing aliasing, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + aliasing.person().getSheetId() + " by=" + aliasing.user()
                + " name=" + aliasing.person().getName() + " alias=" + aliasing.alias());
    }
}
