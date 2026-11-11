package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Rejects a PENDING claim: it is OPEN again, still bound by its person's last statement when that
 * statement included it. Only a steward or the person's approver may reject it.
 */
public class RejectClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RoleRepository roles,
            HttpServletRequest request)
            throws InvalidClaimStatusException, ApprovalException {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.OPEN);
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))
                && !people.find(sheet.id(), claim.getPersonId()).approvedBy(user)) {
            throw new ApprovalException("Only a steward or the person's approver may reject a claim: " + user + " is neither");
        }
        claim.reject();
    }
}
