package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Assigns a REVIEW claim's review to a user name; only a steward may. A blank name or a claim in any other status is
 *  refused. */
public class ApplyReviewAssign {
    public void apply(@Val Claim claim, ClaimRepository repository, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "user", required = false) String userParam) throws ClaimStatusException {
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new ClaimStatusException("Only a steward can assign a review.");
        }
        String assignee = userParam == null ? "" : userParam.trim();
        if (assignee.isEmpty()) {
            throw new ClaimStatusException("A review must be assigned to a user.");
        }
        if (claim.getStatus() != ClaimStatus.REVIEW) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim has no review to assign.");
        }
        claim.setReviewAssigned(assignee);
        repository.saveAndFlush(claim);
    }
}
