package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Stores the claim reminded on the sheet's today; refused when another user moved it on or reminded it first. */
public class ApplyRemind {
    public void apply(@Val Claim claim, ClaimStore store, SheetStore sheets) throws InvalidSheetException {
        if (!store.remind(claim, sheets.today(claim.getSheetId()))) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim not yet reminded today can be reminded");
        }
    }
}
