package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Reminds an OPEN or PENDING claim: its reminded date becomes the sheet's today. A claim in any other status, or one
 * already reminded on the sheet's today, is refused.
 */
public class ApplyRemind {
    public void apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository) throws ClaimStatusException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be reminded.");
        }
        if (sheet.getToday().equals(claim.getRemindedOn())) {
            throw new ClaimStatusException("This claim was already reminded on " + sheet.getToday() + ".");
        }
        claim.setRemindedOn(sheet.getToday());
        repository.saveAndFlush(claim);
    }
}
