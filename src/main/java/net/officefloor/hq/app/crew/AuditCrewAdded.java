package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the crew added to the sheet. */
public class AuditCrewAdded {
    public void audit(@Val Crew crew, @RequestParam("user") String user, Audit audit) {
        audit.record("CREW_ADDED sheet=" + crew.sheetId() + " by=" + user + " name=" + crew.name() + " lead="
                + crew.lead().name());
    }
}
