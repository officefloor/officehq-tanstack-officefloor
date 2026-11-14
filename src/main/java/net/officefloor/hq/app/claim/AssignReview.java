package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the user the claim's review is assigned to, replacing any earlier one. */
public class AssignReview {
    public void assign(@Val Claim claim, @Val ReviewAssignee assignee, ClaimRepository repository) {
        repository.saveReviewAssignee(claim, assignee.user());
    }
}
