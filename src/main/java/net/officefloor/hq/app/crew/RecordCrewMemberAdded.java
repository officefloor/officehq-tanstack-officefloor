package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordCrewMemberAdded {
    public void record(@Val CrewMemberAdded added, @Val ChangedBy by, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + added.sheetId() + " by=" + by.name() + " crew=" + added.crew()
                + " name=" + added.name());
    }
}
