package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.setting.UserRole;
import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Moves the claim to SETTLED, refusing it when its status does not allow that, and fixes its due (see {@link ClaimDue}).
 *  The user who opened the claim (arriving as X-User) cannot settle it. When a clerk (any user not made a steward)
 *  settles a claim whose due is above the sheet's clerk limit (no limit when none is set; a claim with no due
 *  is within it), it goes to PENDING instead, its due still live, for a steward to approve or reject; a steward settles any. */
public class SettleClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, PersonRepository people,
            RateRepository rates, SettingRepository settings, UserRoleRepository roles, SheetRepository sheets)
            throws InvalidClaimStatusException, SelfSettleException {
        String by = user == null ? "" : user.trim();
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by)) {
            throw new SelfSettleException("The user who opened a claim cannot settle it.");
        }
        ClaimTransition.check(claim.getStatus(), ClaimStatus.SETTLED);
        BigDecimal due = ClaimDue.now(claim, people, rates, settings, sheets);
        if (due != null && UserRole.CLERK.equals(roles.roleOf(claim.getSheetId(), by))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId()).orElse(null);
            if (limit != null && due.compareTo(limit) > 0) {
                ClaimTransition.to(claim, ClaimStatus.PENDING);
                return;
            }
        }
        ClaimTransition.to(claim, ClaimStatus.SETTLED);
        claim.setSettledDue(due);
    }
}
