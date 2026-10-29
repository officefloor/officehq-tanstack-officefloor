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
        // A voided SETTLED claim's line carries the frozen figure it takes out of the totals.
        if (loaded.status() == ClaimStatus.SETTLED && status == ClaimStatus.VOID) {
            fields.add("reversed=" + (loaded.settledDue() == null ? "n/a" : loaded.settledDue().toPlainString()));
        }
        // A rejected claim's line carries the steward's reason.
        if (loaded.status() == ClaimStatus.PENDING && status == ClaimStatus.OPEN) {
            fields.add("reason=" + request.reason());
        }
        // A claim voided with a reason from the sheet's list carries it.
        if (status == ClaimStatus.VOID && request.voidReason() != null && !request.voidReason().isEmpty()) {
            fields.add("reason=" + request.voidReason());
        }
        // A claim settled as of a date carries that date.
        if (status == ClaimStatus.SETTLED && request.settleOn() != null) {
            fields.add("on=" + request.settleOn());
        }
        // A reviewed claim is recorded as reviewed (without any number), then as opened.
        if (loaded.status() == ClaimStatus.REVIEW && status == ClaimStatus.OPEN) {
            audit.record(Audit.line(status.event(loaded.status()), loaded.sheetId(), Audit.user(connection),
                    fields.stream().filter(f -> !f.startsWith("number=")).toArray(String[]::new)));
            audit.record(Audit.line(status.event(), loaded.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
            return;
        }
        audit.record(Audit.line(status.event(loaded.status()), loaded.sheetId(),
                Audit.user(connection), fields.toArray(String[]::new)));
    }
}
