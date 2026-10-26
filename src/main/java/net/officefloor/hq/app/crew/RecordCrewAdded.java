package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordCrewAdded {
    public void record(@Val CrewAdded added, @Val ChangedBy by, Audit audit) {
        audit.record("CREW_ADDED sheet=" + added.sheetId() + " by=" + by.name() + " name=" + added.name()
                + " lead=" + added.lead());
    }
}
