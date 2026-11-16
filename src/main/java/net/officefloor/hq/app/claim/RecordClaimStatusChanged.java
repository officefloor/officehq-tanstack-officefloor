package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_OPENED, CLAIM_SETTLED or CLAIM_VOIDED to the audit file once the status is stored. */
public class RecordClaimStatusChanged {
    public void record(@Val Claim claim, @Val ClaimStatusChange change, PersonRepository people, Audit audit) {
        String event = switch (claim.status()) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record(event + " sheet=" + claim.sheetId() + " by=" + change.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date());
    }
}
