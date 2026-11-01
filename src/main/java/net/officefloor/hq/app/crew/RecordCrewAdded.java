package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the crew is saved, so a refused one writes no audit line. */
public class RecordCrewAdded {
    public void record(@Val Crew crew, Audit audit) {
        audit.record("CREW_ADDED sheet=" + crew.sheetId() + " by=" + crew.by() + " name=" + crew.name()
                + " lead=" + crew.leadName());
    }
}
