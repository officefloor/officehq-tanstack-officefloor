package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/**
 * Waives an OPEN or PENDING claim's late charge: a steward's, once. Anyone else, any other status, or a claim already
 * waived is refused.
 */
public class ApplyWaive {
    public void apply(@Val Claim claim, @Val ChangedBy by, ClaimRepository repository, RoleRepository roles)
            throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by.name()))) {
            throw new BadRequestException("Only a steward waives a claim's late charge");
        }
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("Only an OPEN or PENDING claim's late charge is waived; this one is "
                    + claim.getStatus());
        }
        if (claim.isWaived()) {
            throw new BadRequestException("The claim's late charge is already waived");
        }
        if (!repository.waive(claim)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
    }
}
