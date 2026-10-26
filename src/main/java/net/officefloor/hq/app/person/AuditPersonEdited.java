package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonEdited {
    public void audit(@Val Person loaded, @Val PersonResponse edited,
            ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_EDITED", loaded.sheetId(), Audit.user(connection), "name=" + edited.name(),
                "grade=" + edited.grade()));
    }
}
