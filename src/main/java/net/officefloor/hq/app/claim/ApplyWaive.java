package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Waives an OPEN or PENDING claim's late charge; only a steward may. A claim in any other status, or one already
 * waived, is refused. A waived claim bears no late charge from then on, whatever its overdue state.
 */
public class ApplyWaive {
    public void apply(@Val Claim claim, ClaimRepository repository, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws ClaimStatusException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), AuditUser.decode(userHeader)))) {
            throw new ClaimStatusException("Only a steward can waive a claim's late charge.");
        }
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim's late charge cannot be waived.");
        }
        if (claim.isWaived()) {
            throw new ClaimStatusException("This claim's late charge is already waived.");
        }
        claim.setWaived(true);
        repository.saveAndFlush(claim);
    }
}
