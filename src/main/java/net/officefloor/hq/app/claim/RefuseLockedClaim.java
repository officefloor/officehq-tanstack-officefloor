package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Refuses a save of a claim whose editor another user holds open. */
public class RefuseLockedClaim {
    public void check(@Val Claim claim, @Val ChangedBy by, ClaimLockRepository locks) throws ConflictException {
        String holder = locks.holder(claim.getId());
        if (holder != null && !holder.equals(by.name())) {
            throw new ConflictException(ApplyClaimLock.locked(holder));
        }
    }
}
