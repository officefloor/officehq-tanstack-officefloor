package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses the edit unless the claim is a DRAFT, as loaded and as stored now (an open of it that got
 *  there first has committed by then). An OPEN claim is changed by voiding it and adding a new one. */
public class CheckClaimEditable {
    public void check(@Val Claim claim, ClaimRepository repository) throws ClaimNotEditableException {
        if (claim.getStatus() != ClaimStatus.DRAFT
                || ClaimStatus.valueOf(repository.storedStatus(claim.getId())) != ClaimStatus.DRAFT) {
            throw new ClaimNotEditableException("Only a DRAFT claim can be edited; void it and add a new one.");
        }
    }
}
