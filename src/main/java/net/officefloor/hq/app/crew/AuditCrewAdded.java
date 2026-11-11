package net.officefloor.hq.app.crew;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the crew added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditCrewAdded {
    public void audit(@Val Crew crew, HttpServletRequest request, Audit audit) {
        audit.record("CREW_ADDED sheet=" + crew.sheetId() + " by=" + Audit.user(request) + " name=" + crew.name()
                + " lead=" + crew.lead());
    }
}
