package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_ADDED to the audit file once the claim is stored, then LATE_ENTRY for a late entry (see AddClaim). Then writes the
 * figures the change moved (see Rollup). */
public class RecordClaimAdded {
    public void record(@Val Claim claim, @Val Person person, @Val ClaimValues values, @Val RollupBefore before, ClaimRepository claims, Rollup rollup, Audit audit) {
        audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + values.by() + " person=" + person.name()
                + " amount=" + claim.amount().toPlainString() + " date=" + claim.date());
        if (claims.lateEntry(claim.sheetId(), claim.id())) {
            audit.record("LATE_ENTRY sheet=" + claim.sheetId() + " by=" + values.by() + " person=" + person.name()
                    + " amount=" + claim.amount().toPlainString() + " date=" + claim.date());
        }
        rollup.record(claim.sheetId(), values.by(), before);
    }
}
