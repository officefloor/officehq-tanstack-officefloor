package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditCrewAdded {
    public void audit(@Val Crew crew, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("CREW_ADDED", crew.sheetId(), Audit.user(connection), "name=" + crew.name(),
                "lead=" + crew.lead()));
    }
}
