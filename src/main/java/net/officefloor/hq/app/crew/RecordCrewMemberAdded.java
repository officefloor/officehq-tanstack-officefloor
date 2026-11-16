package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes CREW_MEMBER_ADDED to the audit file once the member is stored. */
public class RecordCrewMemberAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val CrewMemberValues values, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " crew=" + values.crew()
                + " name=" + values.name());
    }
}
