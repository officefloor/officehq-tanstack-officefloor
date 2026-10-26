package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Holds an OPEN or PENDING claim, or releases it when held. Any other status is refused. */
public class ApplyClaimHold {
    public void apply(@Val Claim claim, ClaimRepository repository) throws BadRequestException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("Only an OPEN or PENDING claim is held or released; this one is "
                    + claim.getStatus());
        }
        if (!repository.hold(claim, !claim.isHeld())) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
    }
}
