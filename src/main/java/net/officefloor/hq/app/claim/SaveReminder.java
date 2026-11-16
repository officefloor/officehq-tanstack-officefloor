package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores a reminder on the sheet's today, but only while the claim is OPEN or PENDING and not yet reminded that day. */
public class SaveReminder {
    public void save(@Val Claim claim, ClaimRepository claims, SheetRepository sheets)
            throws InvalidClaimStatusException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("Only an OPEN or PENDING claim can be reminded");
        }
        if (!claims.remind(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("The claim was already reminded today");
        }
    }
}
