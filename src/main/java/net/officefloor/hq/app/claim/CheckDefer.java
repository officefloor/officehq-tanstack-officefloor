package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Refuses deferring a claim that is not OPEN, or to a date that is not after the sheet's today. */
public class CheckDefer {
    public void check(@Val DeferInput input, @Val Claim claim, SheetStore sheets) throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new InvalidSheetException("Only an OPEN claim can be deferred");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (!input.until().isAfter(today)) {
            throw new InvalidSheetException("A claim can only be deferred to a date after " + today);
        }
    }
}
