package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimLineAdded {
    // The claim with its new amount (as the screen sees it), then the line.
    public void audit(@Val Claim loaded, @Val ClaimResponse edited, @Val ClaimLine line, People people,
            ServerHttpConnection connection, Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + edited.amount(), "date=" + edited.date()));
        if (edited.number() != null) {
            fields.add("number=" + edited.number());
        }
        fields.add("label=" + line.label());
        fields.add("qty=" + line.qty());
        fields.add("unit=" + line.unit().toPlainString());
        audit.record(Audit.line("CLAIM_LINE_ADDED", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}
