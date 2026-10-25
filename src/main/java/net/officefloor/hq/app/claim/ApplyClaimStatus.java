package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the asked status, refusing a change its current status does not allow. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository)
            throws BadRequestException {
        if (!claim.getStatus().canBecome(status)) {
            throw new BadRequestException("A " + claim.getStatus() + " claim cannot become " + status);
        }
        claim.setStatus(status);
        repository.updateStatus(claim);
    }
}
