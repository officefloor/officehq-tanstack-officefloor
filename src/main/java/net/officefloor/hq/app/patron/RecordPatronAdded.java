package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Writes PATRON_ADDED to the audit file once the patron is stored. */
public class RecordPatronAdded {
    public void record(@Val Person person, @Val PatronAdd add, @Val PatronOf added, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.sheetId() + " by=" + add.by() + " name=" + person.name()
                + " patron=" + added.patron().name());
    }
}
