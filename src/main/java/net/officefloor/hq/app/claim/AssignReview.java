package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Assigns a claim in REVIEW to a user to review, replacing any assigned before. Only a steward may
 * assign it; a claim in any other status is refused. Writes only when every rule passes.
 */
public class AssignReview {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val ReviewAssignment assignment, RoleRepository roles,
            ClaimRepository repository, HttpServletRequest request)
            throws InvalidClaimStatusException, ApprovalException {
        if (claim.getStatus() != ClaimStatus.REVIEW) {
            throw new InvalidClaimStatusException("Only a REVIEW claim is assigned for review, not a "
                    + claim.getStatus() + " one");
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new ApprovalException("Only a steward may assign a review: " + user + " is a "
                    + roles.roleOf(sheet.id(), user));
        }
        claim.setReviewUser(assignment.user());
        if (!repository.assignReview(claim)) {
            throw new InvalidClaimStatusException("The claim was changed by another request");
        }
    }
}
