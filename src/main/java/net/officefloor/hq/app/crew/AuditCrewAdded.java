package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the crew, once it is added. */
public class AuditCrewAdded {
    public void audit(@Val Crew crew, @Val ApplyCrew.CrewLead lead,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("CREW_ADDED sheet=" + crew.getSheetId() + " by=" + AuditUser.decode(user) + " name="
                + crew.getName() + " lead=" + lead.name());
    }
}
