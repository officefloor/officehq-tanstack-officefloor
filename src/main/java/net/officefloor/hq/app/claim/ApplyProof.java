package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one; a SETTLED or VOID claim is refused. */
public class ApplyProof {
    public void apply(@Val Claim claim, ClaimRepository repository) throws ClaimStatusException {
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot gain a proof.");
        }
        claim.addProof();
        repository.saveAndFlush(claim);
    }
}
