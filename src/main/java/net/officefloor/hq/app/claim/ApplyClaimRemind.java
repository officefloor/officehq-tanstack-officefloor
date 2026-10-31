package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim on the sheet's today, once per sheet-today. A claim in any other
 *  status, or one already reminded on the sheet's today, is refused. */
public class ApplyClaimRemind {
    public void apply(@Val ClaimRemind change, @Val Claim claim, Sheets sheets) throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new ClaimStatusRefusedException("The claim has already been reminded on " + today);
        }
        claim.setRemindedOn(today);
    }
}
