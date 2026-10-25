package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the asked status, refusing a change its current status does not allow. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository,
            Out<ClaimNumber> audited) throws BadRequestException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(status)) {
            throw new BadRequestException("A " + from + " claim cannot become " + status);
        }
        Integer had = claim.getNumber();
        claim.setStatus(status);
        if (status == ClaimStatus.VOID) {
            claim.setNumber(null);
        }
        if (!repository.updateStatus(claim, from)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (status == ClaimStatus.OPEN) {
            // Numbered only once the claim is surely moving, so a refused open uses up no number.
            claim.setNumber(repository.nextNumber(claim.getSheetId()));
            repository.updateStatus(claim, status);
        }
        audited.set(new ClaimNumber(status == ClaimStatus.VOID ? had : claim.getNumber()));
    }
}
