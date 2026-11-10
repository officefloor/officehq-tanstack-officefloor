package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Deletes the claim: only a DRAFT may be deleted. A DELETED claim counts nowhere and cannot be edited, opened or voided. */
public class DeleteClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.DELETED);
    }
}
