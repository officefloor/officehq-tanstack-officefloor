package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Holds an OPEN or PENDING claim, or releases a held one. Refused: a claim in any other status. */
public class ApplyClaimHold {
    public void apply(@Val ClaimHold change, @Val Claim claim) throws ClaimStatusRefusedException {
        if (!claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be held or released");
        }
        claim.setHeld(!claim.isHeld());
    }
}
