package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SettleClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.SETTLED);
    }
}
