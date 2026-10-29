package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonEdited {
    public void audit(@Val Person loaded, @Val PersonResponse edited,
            ServerHttpConnection connection, Audit audit) {
        // terms= only when the save changed the person's own terms recorded, showing those in force on the sheet's today
        // ("" for none); contact= when it changed the contact ("" when it cleared it).
        List<String> fields = new ArrayList<>(List.of("name=" + edited.name(), "grade=" + edited.grade()));
        if (!PersonResponse.termses(loaded).equals(edited.termses())) {
            fields.add("terms=" + (edited.terms() == null ? "" : edited.terms()));
        }
        if (!Objects.equals(loaded.contact(), edited.contact())) {
            fields.add("contact=" + (edited.contact() == null ? "" : edited.contact()));
        }
        audit.record(Audit.line("PERSON_EDITED", loaded.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
