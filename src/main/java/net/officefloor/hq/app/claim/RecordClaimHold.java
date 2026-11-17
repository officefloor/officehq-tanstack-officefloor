package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_HELD or CLAIM_RELEASED to the audit file once the change is stored, naming the claim
 *  (number=<number> when it has one), then the figures the change moved (see Rollup). Nothing when nothing changed. */
public class RecordClaimHold {
    public void record(@Val Claim claim, @Val ClaimHold hold, @Val ClaimHoldChange change, PersonRepository people,
            @Val RollupBefore before, Rollup rollup, Audit audit) {
        if (!change.changed()) {
            return;
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record((hold.held() ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + claim.sheetId() + " by=" + hold.by()
                + " person=" + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number()));
        rollup.record(claim.sheetId(), hold.by(), before);
    }
}
