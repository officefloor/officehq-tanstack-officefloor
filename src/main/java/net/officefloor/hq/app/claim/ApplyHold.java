package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * Holds an OPEN or PENDING claim, or releases it when it is held. A claim in any other status is refused. While held,
 * the claim counts in no live total, as a held person's claims do, unless a statement binds it.
 */
public class ApplyHold {
    public void apply(@Val Claim claim, ClaimRepository repository) throws ClaimStatusException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be held or released.");
        }
        claim.setHeld(!claim.isHeld());
        repository.saveAndFlush(claim);
    }
}
