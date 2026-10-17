package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to VOID, refusing it when its status does not allow that, and clears its number
 *  (the number is not given to anyone else). */
public class VoidClaim {
    public void apply(@Val Claim claim, Out<VoidedNumber> voided) throws InvalidClaimStatusException {
        ClaimTransition.to(claim, ClaimStatus.VOID);
        voided.set(new VoidedNumber(claim.getNumber()));
        claim.setNumber(null);
    }
}
