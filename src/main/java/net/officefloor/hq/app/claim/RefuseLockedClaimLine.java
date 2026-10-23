package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse a line on a claim whose editor another user holds open (locked): adding a line is editing the claim. */
public class RefuseLockedClaimLine {
    public void refuse(@Val LineInput input, @Val Claim loaded, ClaimRepository repository)
            throws ClaimStatusNotAllowedException {
        String holder = repository.lockedBy(loaded);
        if (holder != null && !holder.equals(input.user())) {
            throw new ClaimStatusNotAllowedException("This claim is being edited by " + holder
                    + "; no line can be added until they save or cancel.");
        }
    }
}
