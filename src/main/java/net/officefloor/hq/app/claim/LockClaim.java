package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class LockClaim {
    // Opening the editor locks the claim for the user (two users opening it at once: only one gets it).
    public void lock(@Val Claim claim, ServerHttpConnection connection, ClaimLocks locks,
            Out<ClaimResponse> responseOut) throws ClaimLockedException {
        String user = Audit.user(connection).trim();
        if (!locks.lock(claim, user)) {
            throw new ClaimLockedException("The claim is being edited by " + locks.lockedBy(claim) + ".");
        }
        responseOut.set(ClaimResponse.from(claim).withLockedBy(user));
    }
}
