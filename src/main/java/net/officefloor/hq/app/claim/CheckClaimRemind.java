package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) a reminder unless the claim is OPEN or PENDING and was not already reminded on the sheet's today. */
public class CheckClaimRemind {
    public void check(@Val Claim claim, ClaimRepository repository, SheetRepository sheets)
            throws InvalidClaimException {
        if (!claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be reminded.");
        }
        if (sheets.today(claim.sheetId()).equals(repository.reminded(claim.sheetId(), claim.id()))) {
            throw new InvalidClaimException("The claim was already reminded today.");
        }
    }
}
