package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class OpenClaim {
    public void move(@Val Claim claim) throws ClaimStatusException {
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.OPEN).name());
    }
}
