package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses editing any claim but a draft: to change an open claim, void it and add a new one. */
public class CheckClaimEditDraft {
    public void check(@Val Claim claim) throws InvalidClaimStatusException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimStatusException("Only a draft claim can be edited; this one is "
                    + claim.getStatus() + ". To change it, void it and add a new one.");
        }
    }
}
