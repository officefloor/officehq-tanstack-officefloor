package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) assigning the review of a claim that is not in REVIEW, or by anyone but a steward (by delegation too). */
public class CheckReviewAssign {
    public void check(@Val Claim claim, @RequestParam("user") String user, SettingRepository settings)
            throws InvalidClaimException {
        if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can assign a review.");
        }
        if (claim.status() != ClaimStatus.REVIEW) {
            throw new InvalidClaimException("A " + claim.status() + " claim has no review to assign.");
        }
    }
}
