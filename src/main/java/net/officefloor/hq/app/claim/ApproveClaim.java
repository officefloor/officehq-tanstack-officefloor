package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** A steward approves a PENDING claim: it becomes SETTLED with its due fixed at this moment (see {@link ClaimDue}).
 *  Refused for any other status, for a user who is not a steward, and for the user who opened the claim
 *  (arriving as X-User). */
public class ApproveClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, PersonRepository people,
            RateRepository rates, SettingRepository settings, UserRoleRepository roles, SheetRepository sheets)
            throws InvalidClaimStatusException, SelfSettleException, NotStewardException {
        String by = user == null ? "" : user.trim();
        ClaimTransition.requirePending(claim, ClaimStatus.SETTLED);
        Stewards.require(claim, by, roles, "approve");
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by)) {
            throw new SelfSettleException("The user who opened a claim cannot approve it.");
        }
        claim.setSettledDue(ClaimDue.now(claim, people, rates, settings, sheets));
        claim.setStatus(ClaimStatus.SETTLED);
    }
}
