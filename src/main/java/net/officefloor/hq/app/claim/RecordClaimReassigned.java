package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused reassignment writes no audit line. Names the claim
 *  like every claim line, with its new person. */
public class RecordClaimReassigned {
    public void record(@Val ClaimReassignment change, @Val Claim claim, Audit audit) {
        audit.record("CLAIM_REASSIGNED sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + change.to() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + " from=" + change.from() + " to=" + change.to());
    }
}
