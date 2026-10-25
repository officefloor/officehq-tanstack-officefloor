package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPatronAdded {
    public void record(@Val Person person, @Val PatronAdded added, @Val ChangedBy by, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " patron=" + added.patron().getName());
    }
}
