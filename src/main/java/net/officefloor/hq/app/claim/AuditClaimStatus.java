package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored status change in the audit file, with the amount and date as the screen shows them,
 *  and the claim's number (for a void, the number it had); a settle or approve adds the due it fixed
 *  (n/a when the grade had no rate), a reject its reason. A settle that left the claim PENDING is CLAIM_PENDING. */
public class AuditClaimStatus {
    public void audit(@Val ClaimStatusInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        String event = switch (input.action()) {
            case "open" -> "CLAIM_OPENED";
            case "settle" -> "PENDING".equals(saved.status()) ? "CLAIM_PENDING" : "CLAIM_SETTLED";
            case "approve" -> "CLAIM_APPROVED";
            case "reject" -> "CLAIM_REJECTED";
            case "void" -> "CLAIM_VOIDED";
            default -> throw new IllegalStateException("Unknown claim action: " + input.action() + ".");
        };
        boolean settled = "SETTLED".equals(saved.status());
        String number = saved.number() != null ? saved.number() : loaded.number();
        audit.record(event + " sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (number != null ? " number=" + number : "")
                + (settled ? " due=" + (saved.settledDue() != null ? saved.settledDue() : "n/a") : "")
                + (input.reason() != null ? " reason=" + input.reason() : ""));
    }
}
