package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses (400) an edit of any claim that is not a DRAFT: an open claim is changed by voiding it and adding another. */
public class CheckClaimDraft {
    public void check(@Val Claim claim) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be edited; only a DRAFT can.");
        }
    }
}
