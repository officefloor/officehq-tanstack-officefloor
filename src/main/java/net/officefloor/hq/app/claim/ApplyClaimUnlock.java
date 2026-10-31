package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Closes the user's editor on the claim (its cancel button), releasing the lock; another user's lock stays. */
public class ApplyClaimUnlock {
    public void apply(@Val ClaimLock lock, @Val Claim claim) {
        if (lock.user().equals(claim.getLockedBy())) claim.setLockedBy(null);
    }
}
