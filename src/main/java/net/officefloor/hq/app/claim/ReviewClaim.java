package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Reviews a claim in REVIEW: it is OPEN, numbered then ({@link NumberReviewedClaim}). It keeps the user
 * who opened it. Only a steward may review it.
 */
public class ReviewClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, HttpServletRequest request)
            throws InvalidClaimStatusException, ApprovalException {
        if (claim.getStatus() != ClaimStatus.REVIEW) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.OPEN);
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new ApprovalException("Only a steward may review a claim: " + user + " is a "
                    + roles.roleOf(sheet.id(), user));
        }
        claim.moveTo(ClaimStatus.OPEN);
    }
}
