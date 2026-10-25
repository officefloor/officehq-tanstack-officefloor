package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Only a DRAFT claim may be edited; to change an open claim, void it and add a new one. */
public class RefuseNonDraftEdit {
    public void check(@Val Claim claim) throws BadRequestException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BadRequestException("Only a DRAFT claim can be edited; void it and add a new one instead");
        }
    }
}
