package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * Holds an OPEN or PENDING claim, or releases it when held; any other status is refused and writes
 * nothing.
 */
public class HoldClaim {
    public void apply(@Val Claim claim, ClaimRepository repository) throws InvalidClaimStatusException {
        claim.toggleHold();
        if (!repository.hold(claim)) throw new InvalidClaimStatusException("The claim was changed by another request");
    }
}
