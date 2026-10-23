package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored crew in the audit file. */
public class AuditCrewAdded {
    public void audit(@Val CrewAdded added, Audit audit) {
        Crew c = added.crew();
        audit.record("CREW_ADDED sheet=" + c.sheetId() + " by=" + c.by() + " name=" + c.name() + " lead="
                + added.lead());
    }
}
