package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** A claim's reference must not be one another claim on the sheet has, regardless of case. */
public class CheckClaimRef {
    public void check(@Val Claim claim, ClaimRepository repository) throws InvalidClaimException {
        if (claim.getRef() == null) return;
        if (repository.refTaken(claim.getSheetId(), claim.getRef(), claim.getId())) {
            throw new InvalidClaimException("The reference " + claim.getRef() + " is already used on the sheet");
        }
    }
}
