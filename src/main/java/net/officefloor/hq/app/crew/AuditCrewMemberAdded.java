package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the member added to a crew. */
public class AuditCrewMemberAdded {
    public void audit(@Val NewCrewMember added, @RequestParam("user") String user, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + added.crew().sheetId() + " by=" + user + " crew="
                + added.crew().name() + " name=" + added.member().name());
    }
}
