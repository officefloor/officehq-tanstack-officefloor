package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_SHARED to the audit file once the share is stored, naming the claim (number=<number> when it has one),
 *  then with=<person> share=<share>, then the figures the change moved (see Rollup). */
public class RecordClaimShared {
    public void record(@Val Claim claim, @Val ClaimSharing sharing, PersonRepository people,
            @Val RollupBefore before, Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        Person with = people.find(claim.sheetId(), sharing.personId());
        audit.record("CLAIM_SHARED sheet=" + claim.sheetId() + " by=" + sharing.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number())
                + " with=" + (with == null ? "" : with.name()) + " share=" + sharing.share().toPlainString());
        rollup.record(claim.sheetId(), sharing.by(), before);
    }
}
