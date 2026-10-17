package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Moves the claim to VOID, refusing it when its status does not allow that. It keeps its number (the
 *  number is still never given to anyone else). */
public class VoidClaim {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        ClaimTransition.to(claim, ClaimStatus.VOID);
    }
}
