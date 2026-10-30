package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one; not a SETTLED or VOID claim, nor one dated inside a closed period. */
public class ApplyProof {
    public void apply(@Val ProofRequest request, @Val Claim claim, Periods periods) throws ClaimStatusRefusedException {
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) {
            throw new ClaimStatusRefusedException("A " + claim.getStatus() + " claim cannot take a proof");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can take a proof");
        }
        claim.setProofs(claim.getProofs() + 1);
    }
}
