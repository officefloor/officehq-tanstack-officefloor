package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class RefuseNonDraftClaim {
    // Only a draft can be edited; an open claim is changed by voiding it and adding a new one.
    public void refuse(@Val Claim claim) throws ClaimNotDraftException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new ClaimNotDraftException("A " + claim.status()
                    + " claim cannot be edited: only a draft can. Void it and add a new one.");
        }
    }
}
