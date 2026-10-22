package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored status change in the audit file, with the amount and date as the screen shows them. */
public class AuditClaimStatus {
    public void audit(@Val ClaimStatusInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        String event = switch (input.to()) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> throw new IllegalStateException("A claim never returns to DRAFT.");
        };
        audit.record(event + " sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date());
    }
}
