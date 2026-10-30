package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Holds an OPEN or PENDING claim, or releases it when held; a claim in any other status is refused. */
public class ApplyHoldClaim {
    public void apply(@Val HoldClaimRequest request, @Val Claim claim) throws ClaimStatusRefusedException {
        if (!claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be held or released");
        }
        claim.setHeld(!claim.isHeld());
    }
}
