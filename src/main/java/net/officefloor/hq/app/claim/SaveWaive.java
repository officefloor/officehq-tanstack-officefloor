package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the waiver as made on the sheet's today, but only while the claim is still OPEN or PENDING and not waived; a
 *  change another user made first is refused. */
public class SaveWaive {
    public void save(@Val Claim claim, ClaimRepository claims, SheetRepository sheets) throws InvalidClaimStatusException {
        if (!claims.waive(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
    }
}
