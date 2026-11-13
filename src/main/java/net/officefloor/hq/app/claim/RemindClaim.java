package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the sheet's today as the day the claim was last reminded. */
public class RemindClaim {
    public void remind(@Val Claim claim, ClaimRepository repository, SheetRepository sheets) {
        repository.remind(claim.sheetId(), claim.id(), sheets.today(claim.sheetId()));
    }
}
