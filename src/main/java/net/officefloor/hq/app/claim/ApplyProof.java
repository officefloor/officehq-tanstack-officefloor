package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyProof {
    // A SETTLED, VOID or DELETED claim takes no more proofs. Stored only if the claim still has its loaded status and count, so
    // a claim settled or voided meanwhile by another user is refused.
    public void apply(@Val Claim claim, Claims claims, Out<ClaimResponse> responseOut) throws ClaimProofRefusedException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID
                || claim.status() == ClaimStatus.DELETED) {
            throw new ClaimProofRefusedException("A " + claim.status() + " claim takes no more proofs.");
        }
        Claim proved = claims.addProof(claim);
        if (proved == null) {
            throw new ClaimProofRefusedException("The claim changed meanwhile: try again.");
        }
        responseOut.set(ClaimResponse.from(proved));
    }
}
