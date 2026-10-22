package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse an edit of a claim whose editor another user holds open (locked). */
public class RefuseLockedClaimEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, ClaimRepository repository)
            throws ClaimStatusNotAllowedException {
        String holder = repository.lockedBy(loaded);
        if (holder != null && !holder.equals(input.user())) {
            throw new ClaimStatusNotAllowedException("This claim is being edited by " + holder
                    + "; it cannot be edited until they save or cancel.");
        }
    }
}
