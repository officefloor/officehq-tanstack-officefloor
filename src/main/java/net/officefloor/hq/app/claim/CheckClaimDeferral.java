package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) a deferral unless the claim is OPEN and the date is after the sheet's today. */
public class CheckClaimDeferral {
    public void check(@Val Claim claim, @Val Deferral deferral, SheetRepository sheets) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be deferred.");
        }
        if (!deferral.until().isAfter(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimException("A claim can only be deferred to a date after the sheet's today.");
        }
    }
}
