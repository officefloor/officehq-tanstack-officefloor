package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimStatus {
    public void audit(@Val Claim loaded, @Val ClaimResponse changed, People people,
            ServerHttpConnection connection, Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + changed.amount(), "date=" + changed.date()));
        // A voided claim's line carries the number it had.
        String number = changed.number() != null ? changed.number() : loaded.number();
        if (number != null) {
            fields.add("number=" + number);
        }
        // A settled claim's line carries the due it was fixed at.
        if (ClaimStatus.valueOf(changed.status()) == ClaimStatus.SETTLED) {
            fields.add("due=" + (changed.settledDue() == null ? "n/a" : changed.settledDue()));
        }
        audit.record(Audit.line(ClaimStatus.valueOf(changed.status()).event(), loaded.sheetId(),
                Audit.user(connection), fields.toArray(String[]::new)));
    }
}
