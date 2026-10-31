package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.plugin.variable.Val;

/** Opens the claim's editor for the user, locking the claim for everyone else; refused (taking no
 *  lock) when another user's editor locks it, it is no longer a DRAFT, or it is dated in a closed period. */
public class ApplyClaimLock {
    public void apply(@Val ClaimLock lock, @Val Claim claim, ClosedPeriods closedPeriods)
            throws ClaimLockedException, ClaimEditRefusedException, PeriodClosedException {
        refuseLocked(claim, lock.user());
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimEditRefusedException(claim.getStatus());
        }
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        claim.setLockedBy(lock.user());
    }

    /** Refuse a change by `user` to a claim another user's open editor locks. */
    static void refuseLocked(Claim claim, String user) throws ClaimLockedException {
        String holder = claim.getLockedBy();
        if (holder != null && !holder.equals(user == null ? null : user.trim())) {
            throw new ClaimLockedException(holder);
        }
    }
}
