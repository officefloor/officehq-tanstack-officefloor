package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Saves the claim's lock; the user's editor on any other claim of the same person is closed, its lock released. */
public class SaveClaimLock {
    public void save(@Val ClaimLock lock, @Val Claim claim, Claims claims) {
        claims.unlockOthers(claim.getSheetId(), claim.getPersonId(), lock.user(), claim.getId());
        claims.save(claim);
    }
}
