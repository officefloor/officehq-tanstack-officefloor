package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_OPENED, CLAIM_SETTLED or CLAIM_VOIDED to the audit file once the status is stored, ending
 * with number=<number> when the claim has (or, voided, had) one; CLAIM_SETTLED then ends with the due it fixed
 * (due=n/a when its grade had no rate). Then writes the
 * figures the change moved (see Rollup). */
public class RecordClaimStatusChanged {
    public void record(@Val Claim claim, @Val ClaimStatusChange change, @Val ClaimNumber number,
            PersonRepository people, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String event = switch (claim.status()) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record(event + " sheet=" + claim.sheetId() + " by=" + change.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (number.value() == null ? "" : " number=" + number.value())
                + (claim.status() == ClaimStatus.SETTLED
                        ? " due=" + (claim.due() == null ? "n/a" : claim.due().toPlainString()) : ""));
        rollup.record(claim.sheetId(), change.by(), before);
    }
}
