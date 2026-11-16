package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_DEFERRED to the audit file once the deferral is stored, naming the claim (number=<number> when it has
 *  one) and ending with until=<date>, then the figures the change moved (see Rollup). */
public class RecordClaimDeferred {
    public void record(@Val Claim claim, @Val ClaimDeferral deferral, PersonRepository people,
            @Val RollupBefore before, Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_DEFERRED sheet=" + claim.sheetId() + " by=" + deferral.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number())
                + " until=" + deferral.until());
        rollup.record(claim.sheetId(), deferral.by(), before);
    }
}
