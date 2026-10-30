package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one; not a SETTLED, VOID or DELETED claim, nor one whose due-by is inside a closed period. */
public class ApplyProof {
    public void apply(@Val ProofRequest request, @Val Claim claim, Periods periods) throws ClaimStatusRefusedException {
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID
                || claim.getStatus() == ClaimStatus.DELETED) {
            throw new ClaimStatusRefusedException("A " + claim.getStatus() + " claim cannot take a proof");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getPeriodDate())) {
            throw new ClaimStatusRefusedException("No claim whose due-by is inside a closed period can take a proof");
        }
        claim.setProofs(claim.getProofs() + 1);
    }
}
