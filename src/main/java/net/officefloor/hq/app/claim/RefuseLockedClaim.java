package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class RefuseLockedClaim {
    // A claim whose editor another user has open cannot be edited until they save or cancel it.
    public void refuse(@Val Claim claim, ServerHttpConnection connection, ClaimLocks locks) throws ClaimLockedException {
        String by = locks.lockedBy(claim);
        if (by != null && !by.equals(Audit.user(connection).trim())) {
            throw new ClaimLockedException("The claim is being edited by " + by + ".");
        }
    }
}
