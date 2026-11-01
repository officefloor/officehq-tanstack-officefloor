package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the member is saved, so a refused one writes no audit line. */
public class RecordCrewMemberAdded {
    public void record(@Val CrewMember member, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + member.by() + " crew=" + member.crew()
                + " name=" + member.name());
    }
}
