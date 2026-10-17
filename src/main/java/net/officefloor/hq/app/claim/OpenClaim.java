package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Moves the claim to OPEN, refusing it when its status does not allow that. */
public class OpenClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        ClaimTransition.to(claim, ClaimStatus.OPEN);
    }
}
