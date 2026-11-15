package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a member that went through is recorded. */
public class RecordCrewMemberAdded {
    public void record(@Val CrewMemberAdded member, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + member.by() + " crew="
                + member.crew() + " name=" + member.name());
    }
}
