package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Refuses saving a claim's editor while another user holds the claim locked. */
public class GuardClaimLock {
    public void guard(@Val Claim claim, @RequestHeader(name = "X-User", required = false) String user)
            throws ClaimLockedException {
        if (claim.isLockedFor(AuditUser.decode(user))) {
            throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy() + ".");
        }
    }
}
