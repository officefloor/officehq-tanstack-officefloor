package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_SPLIT to the audit file once the split is stored, naming the original claim with its remaining amount
 *  and ending into=<new number>, then CLAIM_OPENED for the new claim, then the figures the change moved (see Rollup). */
public class RecordClaimSplit {
    public void record(@Val Claim claim, @Val SplitOff made, @Val ClaimSplit split, PersonRepository people,
            @Val RollupBefore before, Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        String name = person == null ? "" : person.name();
        Claim added = made.claim();
        audit.record("CLAIM_SPLIT sheet=" + claim.sheetId() + " by=" + split.by() + " person=" + name + " amount="
                + claim.amount().toPlainString() + " date=" + claim.date()
                + (claim.number() == null ? "" : " number=" + claim.number()) + " into=" + added.number());
        audit.record("CLAIM_OPENED sheet=" + added.sheetId() + " by=" + split.by() + " person=" + name + " amount="
                + added.amount().toPlainString() + " date=" + added.date() + " number=" + added.number());
        rollup.record(claim.sheetId(), split.by(), before);
    }
}
