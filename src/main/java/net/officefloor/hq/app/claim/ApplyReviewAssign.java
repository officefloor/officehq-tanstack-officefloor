package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Checks an assignment of a claim for review: only a steward assigns, only a REVIEW claim, and only to a user named. */
public class ApplyReviewAssign {
    public void apply(@Val Claim claim, @Val ClaimReviewAssignment assignment, RoleRepository roles)
            throws InvalidClaimStatusException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(claim.sheetId(), assignment.by()))) {
            throw new InvalidClaimStatusException("Only a steward assigns a claim for review");
        }
        if (claim.status() != ClaimStatus.REVIEW) {
            throw new InvalidClaimStatusException("Only a REVIEW claim is assigned for review");
        }
        if (assignment.user().isEmpty()) {
            throw new InvalidClaimStatusException("A claim is assigned for review to a user");
        }
    }
}
