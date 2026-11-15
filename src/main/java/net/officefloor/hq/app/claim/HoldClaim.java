package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Holds an OPEN or PENDING claim, or releases it when held; refused for a claim in any other status. */
public class HoldClaim {
    public void hold(@Val Claim claim) throws ClaimStatusException {
        if (!ClaimStatus.isOpen(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be "
                    + (claim.isHeld() ? "released" : "held"));
        }
        claim.setHeld(!claim.isHeld());
    }
}
