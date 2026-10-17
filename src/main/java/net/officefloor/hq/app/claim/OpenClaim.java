package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Moves the claim to OPEN, refusing it when its status does not allow that, and gives it the sheet's
 *  next number and records who opened it (the signed-in user, arriving as X-User). */
public class OpenClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, ClaimRepository repository) throws InvalidClaimStatusException {
        ClaimTransition.check(claim.getStatus(), ClaimStatus.OPEN);
        // Taking the number holds the sheet's numbers until this change commits or is refused. Then
        // re-check the stored status: an open of the same claim that got there first has committed by
        // now, so this one is refused (and its number rolls back with it).
        repository.takeClaimNumber(claim.getSheetId());
        ClaimTransition.check(ClaimStatus.valueOf(repository.storedStatus(claim.getId())), ClaimStatus.OPEN);
        int number = repository.lastClaimNumber(claim.getSheetId());
        ClaimTransition.to(claim, ClaimStatus.OPEN);
        claim.setNumber(number);
        claim.setOpenedBy(user == null ? "" : user.trim());
    }
}
