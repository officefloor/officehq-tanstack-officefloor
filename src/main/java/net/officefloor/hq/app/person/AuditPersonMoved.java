package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonMoved {
    public void audit(@Val MovedPerson moved, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_MOVED", moved.person().sheetId(), Audit.user(connection),
                "name=" + moved.person().name(), "position=" + moved.position()));
    }
}
