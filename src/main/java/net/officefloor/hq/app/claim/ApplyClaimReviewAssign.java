package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;

/** Assigns a REVIEW claim's review to a user. Only a steward (or a user acting as one by delegation)
 *  assigns it; a claim in any other status is refused. The claim keeps the assignment once it leaves REVIEW. */
public class ApplyClaimReviewAssign {
    public void apply(@Val ClaimReviewAssign change, @Val Claim claim, Settings settings) throws ClaimStatusRefusedException {
        if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can assign a claim's review");
        }
        if (claim.getStatus() != ClaimStatus.REVIEW) {
            throw new ClaimStatusRefusedException("Only a REVIEW claim's review can be assigned");
        }
        claim.setReviewAssigned(change.assignee());
    }
}
