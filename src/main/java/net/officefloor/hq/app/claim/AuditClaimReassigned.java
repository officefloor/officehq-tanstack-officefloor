package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimReassigned {
    // Named like every claim line, by its new person, then where it moved from and to.
    public void audit(@Val Claim loaded, @Val ClaimResponse moved, @Val Person to, People people,
            ServerHttpConnection connection, Audit audit) {
        Person from = people.find(loaded.sheetId(), loaded.personId());
        String fromName = from == null ? "" : from.name();
        List<String> fields = new ArrayList<>(List.of("person=" + to.name(), "amount=" + moved.amount(),
                "date=" + moved.date()));
        if (moved.number() != null) {
            fields.add("number=" + moved.number());
        }
        fields.add("from=" + fromName);
        fields.add("to=" + to.name());
        audit.record(Audit.line("CLAIM_REASSIGNED", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}
