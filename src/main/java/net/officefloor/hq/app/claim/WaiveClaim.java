package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Waives an OPEN or PENDING claim's late charge, once: only a steward may; any other status, or a
 * claim already waived, is refused and writes nothing.
 */
public class WaiveClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, ClaimRepository repository,
            HttpServletRequest request) throws StewardOnlyException, InvalidClaimStatusException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!Role.STEWARD.equals(role)) {
            throw new StewardOnlyException("Only a steward may waive a late charge: " + user + " is a " + role);
        }
        claim.waive();
        if (!repository.waive(claim)) throw new InvalidClaimStatusException("The claim's late charge was already waived");
    }
}
