package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Drops the user's lock on the claim: its editor was saved or cancelled. */
public class ReleaseClaimLock {
    public void release(@Val Claim claim, @Val ChangedBy by, ClaimLockRepository locks) {
        locks.release(claim.getId(), by.name());
    }
}
