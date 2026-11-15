package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a crew that went through is recorded. */
public class RecordCrewAdded {
    public void record(@Val CrewAdded crew, Audit audit) {
        audit.record("CREW_ADDED sheet=" + crew.sheetId() + " by=" + crew.by() + " name=" + crew.name()
                + " lead=" + crew.lead());
    }
}
