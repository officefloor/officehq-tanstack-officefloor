package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyHold {
    // Toggles the hold: holds a claim not held, releases a held one. Only an OPEN or PENDING claim, and stored only if it
    // is still OPEN or PENDING and as it was, so a claim settled or toggled meanwhile by another user is refused.
    public void apply(@Val Claim claim, Claims claims, Out<ClaimResponse> responseOut) throws ClaimHoldRefusedException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new ClaimHoldRefusedException("Only an OPEN or PENDING claim can be held or released.");
        }
        Claim toggled = claims.hold(claim, !claim.held());
        if (toggled == null) {
            throw new ClaimHoldRefusedException("The claim changed meanwhile: try again.");
        }
        responseOut.set(ClaimResponse.from(toggled));
    }
}
