package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the share as made on the sheet's today, but only while the claim is still not SETTLED, VOID or DELETED; a
 *  change another user made first is refused. */
public class SaveShare {
    public void save(@Val Claim claim, @Val ClaimSharing sharing, ClaimRepository claims, SheetRepository sheets)
            throws InvalidClaimStatusException {
        if (!claims.share(claim, sharing.personId(), sharing.share(), sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
    }
}
