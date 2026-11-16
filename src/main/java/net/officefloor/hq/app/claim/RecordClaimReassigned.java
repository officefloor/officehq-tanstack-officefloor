package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_REASSIGNED to the audit file once the move is stored, naming the claim as every claim line does (with
 * its new person, and number=<number> when it has one), ending from=<old name> to=<new name>. Then writes the figures
 * the move moved (see Rollup). */
public class RecordClaimReassigned {
    public void record(@Val Claim claim, @Val ClaimReassignment reassignment, @Val RollupBefore before, Rollup rollup,
            Audit audit) {
        audit.record("CLAIM_REASSIGNED sheet=" + claim.sheetId() + " by=" + reassignment.by() + " person="
                + reassignment.to().name() + " amount=" + claim.amount().toPlainString() + " date=" + claim.date()
                + (claim.number() == null ? "" : " number=" + claim.number()) + " from=" + reassignment.from().name()
                + " to=" + reassignment.to().name());
        rollup.record(claim.sheetId(), reassignment.by(), before);
    }
}
