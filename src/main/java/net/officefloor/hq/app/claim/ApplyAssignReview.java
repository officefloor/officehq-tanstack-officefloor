package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's review assigned to the user; refused when the claim is not (or no longer) in REVIEW. */
public class ApplyAssignReview {
    public void apply(@Val AssignReviewInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.REVIEW || !store.assignReview(claim, input.assignee())) {
            throw new InvalidSheetException("Only a REVIEW claim can have its review assigned");
        }
    }
}
