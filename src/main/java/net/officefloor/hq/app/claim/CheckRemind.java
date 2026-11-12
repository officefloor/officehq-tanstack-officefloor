package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Refuses reminding a claim that is not OPEN or PENDING, or one already reminded on the sheet's today. */
public class CheckRemind {
    public void check(@Val Claim claim, SheetStore sheets) throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new InvalidSheetException("The claim was already reminded on " + today);
        }
    }
}
