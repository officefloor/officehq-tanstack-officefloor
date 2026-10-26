package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimStatus {
    public void audit(@Val Claim loaded, @Val ClaimResponse changed, @Val ClaimStatusRequest request, People people,
            ServerHttpConnection connection, Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + changed.amount(), "date=" + changed.date()));
        // A voided claim's line carries the number it had.
        String number = changed.number() != null ? changed.number() : loaded.number();
        if (number != null) {
            fields.add("number=" + number);
        }
        ClaimStatus status = ClaimStatus.valueOf(changed.status());
        // A settled (or approved) claim's line carries the due it was fixed at.
        if (status == ClaimStatus.SETTLED) {
            fields.add("due=" + (changed.settledDue() == null ? "n/a" : changed.settledDue()));
        }
        // A rejected claim's line carries the steward's reason.
        if (loaded.status() == ClaimStatus.PENDING && status == ClaimStatus.OPEN) {
            fields.add("reason=" + request.reason());
        }
        audit.record(Audit.line(status.event(loaded.status()), loaded.sheetId(),
                Audit.user(connection), fields.toArray(String[]::new)));
    }
}
