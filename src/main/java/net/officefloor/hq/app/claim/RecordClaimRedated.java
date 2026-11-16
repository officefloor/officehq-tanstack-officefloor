package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_REDATED to the audit file once the new date is stored, naming the claim as every claim line does (its
 *  new date, and number=<number> as it now is), ending old=<old date> new=<new date>. Then writes the figures the
 *  re-dating moved (see Rollup). */
public class RecordClaimRedated {
    public void record(@Val Claim claim, @Val ClaimRedating redating, @Val RollupBefore before, PersonRepository people,
            Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_REDATED sheet=" + claim.sheetId() + " by=" + redating.by() + " person=" + person.name()
                + " amount=" + claim.amount().toPlainString() + " date=" + claim.date()
                + (claim.number() == null ? "" : " number=" + claim.number()) + " old=" + redating.old() + " new="
                + redating.date());
        rollup.record(claim.sheetId(), redating.by(), before);
    }
}
