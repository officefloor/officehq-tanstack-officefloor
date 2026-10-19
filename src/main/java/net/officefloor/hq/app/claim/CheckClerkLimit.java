package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.ClerkLimit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses a clerk settling a claim whose due (as the settle fixed it) is above the sheet's clerk limit.
 *  A due of n/a is within any limit; a steward settles anything; no limit means none applies. */
public class CheckClerkLimit {
    public void check(@Val ClaimAction action, @Val Claim claim, RoleRepository roles, ClerkLimit clerkLimit,
            @RequestParam(value = "user", required = false) String user) throws InvalidClaimStatusException {
        if (action != ClaimAction.SETTLE || claim.getSettledDue() == null) {
            return;
        }
        if (Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            return;
        }
        var limit = clerkLimit.of(claim.getSheetId());
        if (limit.isPresent() && claim.getSettledDue().compareTo(limit.get()) > 0) {
            throw new InvalidClaimStatusException("A clerk can settle a claim only when its due is at or below the clerk limit of "
                    + limit.get().toPlainString() + "; this claim's due is " + claim.getSettledDue().toPlainString() + ".");
        }
    }
}
