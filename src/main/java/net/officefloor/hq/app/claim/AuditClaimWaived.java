package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimWaived {
    public void audit(@Val Claim loaded, @Val ClaimResponse waived, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + waived.amount(), "date=" + waived.date()));
        if (waived.number() != null) {
            fields.add("number=" + waived.number());
        }
        audit.record(Audit.line("CLAIM_WAIVED", loaded.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
