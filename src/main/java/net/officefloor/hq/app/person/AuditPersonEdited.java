package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonEdited {
    public void audit(@Val Person loaded, @Val PersonResponse edited,
            ServerHttpConnection connection, Audit audit) {
        // terms= only when the save changed the person's own terms ("" when it cleared them).
        if (java.util.Objects.equals(loaded.terms(), edited.terms())) {
            audit.record(Audit.line("PERSON_EDITED", loaded.sheetId(), Audit.user(connection), "name=" + edited.name(),
                    "grade=" + edited.grade()));
        } else {
            audit.record(Audit.line("PERSON_EDITED", loaded.sheetId(), Audit.user(connection), "name=" + edited.name(),
                    "grade=" + edited.grade(), "terms=" + (edited.terms() == null ? "" : edited.terms())));
        }
    }
}
