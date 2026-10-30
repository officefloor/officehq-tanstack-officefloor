package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** A saved editor releases the claim's lock. */
public class ReleaseEditLock {
    public void release(@Val Claim claim, Claims claims) {
        claims.unlock(claim);
    }
}
