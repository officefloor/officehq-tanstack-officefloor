package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the deferral, but only while the claim is still OPEN, logged as made on the sheet's today; a change another user made first is refused. */
public class SaveDefer {
    public void save(@Val Claim claim, ClaimRepository claims, SheetRepository sheets, Out<Claim> saved) throws InvalidClaimStatusException {
        if (!claims.defer(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claim);
    }
}
