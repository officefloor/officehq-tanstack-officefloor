package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** A saved edit closes the claim's editor: its lock is released. */
public class ReleaseClaimLock {
    public void release(@Val Claim claim, ClaimRepository repository) {
        repository.unlock(claim, null);
    }
}
