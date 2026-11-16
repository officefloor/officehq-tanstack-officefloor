package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Releases the claim's lock when the user closing its editor holds it; a lock someone else holds is kept. Publishes
 *  the claim as stored. */
public class ReleaseLock {
    public void release(@Val Claim claim, @Val ClaimLock lock, ClaimRepository claims, Out<Claim> saved) {
        claims.unlock(claim, lock.by());
        saved.set(claim);
    }
}
