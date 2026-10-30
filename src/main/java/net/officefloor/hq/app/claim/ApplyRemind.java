package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim, once per sheet-today: its reminded date becomes the sheet's today. */
public class ApplyRemind {
    public void apply(@Val RemindRequest request, @Val Claim claim, Sheets sheets) throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new ClaimStatusRefusedException("A claim can be reminded only once per sheet-today");
        }
        claim.setRemindedOn(today);
    }
}
