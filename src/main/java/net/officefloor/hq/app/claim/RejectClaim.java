package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** A steward rejects a PENDING claim (with a reason): it goes back to OPEN, its due live again. Refused for any
 *  other status and for a user who is not a steward (arriving as X-User). */
public class RejectClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, UserRoleRepository roles)
            throws InvalidClaimStatusException, NotStewardException {
        ClaimTransition.requirePending(claim, ClaimStatus.OPEN);
        Stewards.require(claim, user == null ? "" : user.trim(), roles, "reject");
        claim.setStatus(ClaimStatus.OPEN);
    }
}
