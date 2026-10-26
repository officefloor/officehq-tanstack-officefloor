package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonHeld {
    public void audit(@Val Person loaded, @Val PersonResponse toggled, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line(toggled.held() ? "PERSON_HELD" : "PERSON_RELEASED", loaded.sheetId(),
                Audit.user(connection), "name=" + toggled.name()));
    }
}
