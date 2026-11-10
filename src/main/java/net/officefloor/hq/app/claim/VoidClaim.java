package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class VoidClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.VOID);
    }
}
