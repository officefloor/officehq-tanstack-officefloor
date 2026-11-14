package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses (400) holding or releasing a claim that is not OPEN or PENDING. */
public class CheckClaimHold {
    public void check(@Val Claim claim) throws InvalidClaimException {
        if (!claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be held or released.");
        }
    }
}
