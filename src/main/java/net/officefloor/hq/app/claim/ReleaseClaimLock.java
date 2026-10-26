package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ReleaseClaimLock {
    // Saving or cancelling the editor releases the user's lock on the claim.
    public void release(@Val Claim claim, ServerHttpConnection connection, ClaimLocks locks) {
        locks.release(claim, Audit.user(connection).trim());
    }
}
