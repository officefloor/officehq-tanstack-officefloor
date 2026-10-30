package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Closes the user's editor on the claim: releases the lock when it is theirs. */
public class ReleaseLock {
    public void release(@Val LockRequest request, @Val Claim claim, Claims claims) {
        if (request.user() != null && request.user().equals(claims.lockedBy(claim))) {
            claims.unlock(claim);
        }
    }
}
