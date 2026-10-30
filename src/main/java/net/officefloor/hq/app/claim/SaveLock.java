package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveLock {
    public void save(@Val LockRequest request, @Val Claim claim, Claims claims) {
        claims.lock(claim, request.user());
    }
}
