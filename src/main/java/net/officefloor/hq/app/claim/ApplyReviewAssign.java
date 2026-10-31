package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;

/** Assigns a REVIEW claim to a user (trimmed, not blank); only a steward assigns. */
public class ApplyReviewAssign {
    public void apply(@Val ReviewAssignRequest request, @Val Claim claim, Settings settings) throws ClaimStatusRefusedException {
        if (!Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()))) {
            throw new ClaimStatusRefusedException("Only a steward can assign a claim's review");
        }
        if (claim.getStatus() != ClaimStatus.REVIEW) {
            throw new ClaimStatusRefusedException("Only a REVIEW claim can be assigned");
        }
        String reviewer = request.reviewer() == null ? "" : request.reviewer().trim();
        if (reviewer.isEmpty()) {
            throw new ClaimStatusRefusedException("A review is assigned to a user");
        }
        claim.setReviewUser(reviewer);
    }
}
