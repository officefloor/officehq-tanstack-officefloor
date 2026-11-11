package net.officefloor.hq.app.crew;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the member added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditCrewMemberAdded {
    public void audit(@Val CrewMember member, HttpServletRequest request, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + Audit.user(request)
                + " crew=" + member.crew() + " name=" + member.name());
    }
}
