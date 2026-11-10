package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Rejects a PENDING claim: it is OPEN again, bound again by its person's last statement when that
 * statement included it. Only a steward may reject it.
 */
public class RejectClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, HttpServletRequest request)
            throws InvalidClaimStatusException, ApprovalException {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.OPEN);
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new ApprovalException("Only a steward may reject a claim: " + user + " is a clerk");
        }
        claim.reject();
    }
}
