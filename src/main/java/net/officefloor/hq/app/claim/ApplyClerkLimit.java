package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.ClerkLimit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A clerk settling a claim whose due (as the settle fixed it) is above the sheet's clerk limit sends it
 *  to PENDING instead, with no fixed due, for a steward to approve or reject.
 *  A due of n/a is within any limit; a steward settles anything; no limit means none applies. */
public class ApplyClerkLimit {
    public void apply(@Val ClaimAction action, @Val Claim claim, RoleRepository roles, ClerkLimit clerkLimit,
            @RequestParam(value = "user", required = false) String user) {
        if (action != ClaimAction.SETTLE || claim.getSettledDue() == null) {
            return;
        }
        if (Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            return;
        }
        var limit = clerkLimit.of(claim.getSheetId());
        if (limit.isPresent() && claim.getSettledDue().compareTo(limit.get()) > 0) {
            claim.setStatus(ClaimStatus.PENDING);
            claim.setSettledDue(null);
        }
    }
}
