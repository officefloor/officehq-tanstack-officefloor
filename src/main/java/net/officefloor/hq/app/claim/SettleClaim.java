package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SettleClaim {
    public void move(@Val Claim claim) throws ClaimStatusException {
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.SETTLED).name());
    }
}
