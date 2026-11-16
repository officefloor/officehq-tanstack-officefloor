package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_WAIVED to the audit file once the waiver is stored, naming the claim (number=<number> when it has
 *  one), then the figures the change moved (see Rollup). */
public class RecordClaimWaived {
    public void record(@Val Claim claim, @Val ClaimWaiver waiver, PersonRepository people,
            @Val RollupBefore before, Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_WAIVED sheet=" + claim.sheetId() + " by=" + waiver.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number()));
        rollup.record(claim.sheetId(), waiver.by(), before);
    }
}
