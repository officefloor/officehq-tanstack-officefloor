package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) an edit of a claim another user has locked by opening its editor. */
public class CheckClaimNotLocked {
    public void check(@Val Claim claim, @RequestParam("user") String user, ClaimLockRepository locks)
            throws InvalidClaimException {
        String holder = locks.lockedBy(claim.sheetId(), claim.id());
        if (holder != null && !holder.equals(user)) {
            throw new InvalidClaimException("The claim is being edited by " + holder + ".");
        }
    }
}
