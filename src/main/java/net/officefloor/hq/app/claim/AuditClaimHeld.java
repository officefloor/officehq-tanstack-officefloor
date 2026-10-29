package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimHeld {
    // CLAIM_HELD when the claim was held, CLAIM_RELEASED when it was released.
    public void audit(@Val Claim loaded, @Val ClaimResponse toggled, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + toggled.amount(), "date=" + toggled.date()));
        if (toggled.number() != null) {
            fields.add("number=" + toggled.number());
        }
        audit.record(Audit.line(toggled.held() ? "CLAIM_HELD" : "CLAIM_RELEASED", loaded.sheetId(),
                Audit.user(connection), fields.toArray(String[]::new)));
    }
}
