package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the patron is saved. */
public class RecordPatronAdded {
    public void record(@Val Patronage patronage, Audit audit) {
        audit.record("PATRON_ADDED sheet=" + patronage.person().getSheetId() + " by=" + patronage.user()
                + " name=" + patronage.person().getName() + " patron=" + patronage.patron().getName());
    }
}
