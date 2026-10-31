package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one. A SETTLED or VOID claim, or one dated in a closed period, is refused. */
public class ApplyClaimProof {
    public void apply(@Val ClaimProof change, @Val Claim claim, ClosedPeriods closedPeriods)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) {
            throw new ClaimStatusRefusedException("A proof cannot be added to a " + claim.getStatus() + " claim");
        }
        claim.setProofs(claim.getProofs() + 1);
    }
}
