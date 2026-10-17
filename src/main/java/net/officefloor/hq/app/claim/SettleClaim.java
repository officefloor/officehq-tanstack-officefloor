package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Moves the claim to SETTLED, refusing it when its status does not allow that. */
public class SettleClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        ClaimTransition.to(claim, ClaimStatus.SETTLED);
    }
}
