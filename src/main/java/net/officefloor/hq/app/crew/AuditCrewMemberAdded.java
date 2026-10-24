package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the crew member, once they are added. */
public class AuditCrewMemberAdded {
    public void audit(@Val Crew crew, @Val ApplyCrewMember.MemberName name,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + crew.getSheetId() + " by=" + AuditUser.decode(user)
                + " crew=" + crew.getName() + " name=" + name.name());
    }
}
