package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the user the claim is assigned to for review, but only while it is still REVIEW; a change another user made
 *  first is refused. */
public class SaveReviewAssign {
    public void save(@Val Claim claim, @Val ClaimReviewAssignment assignment, ClaimRepository claims)
            throws InvalidClaimStatusException {
        if (!claims.assignReview(claim, assignment.user())) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
    }
}
