package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored crew member in the audit file. */
public class AuditCrewMemberAdded {
    public void audit(@Val CrewMember m, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + m.sheetId() + " by=" + m.by() + " crew=" + m.crew() + " name="
                + m.name());
    }
}
