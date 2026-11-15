package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveReviewAssignee {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.updateReviewAssignee(claim);
    }
}
