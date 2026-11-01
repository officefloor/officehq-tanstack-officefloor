package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim on the sheet's today, once per sheet-today. A claim in any other
 *  status, or one already reminded on the sheet's today, is refused. While the sheet has an escalation gap,
 *  a claim not yet escalated whose previous reminder was at least that many days before the sheet's today
 *  is escalated. */
public class ApplyClaimRemind {
    public void apply(@Val ClaimRemind change, @Val Claim claim, Sheets sheets, Settings settings) throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new ClaimStatusRefusedException("The claim has already been reminded on " + today);
        }
        Integer gap = settings.escalateAfter(claim.getSheetId());
        LocalDate previous = claim.getRemindedOn();
        if (gap != null && !claim.isEscalated() && previous != null && !previous.plusDays(gap).isAfter(today)) {
            claim.setEscalated(true);
            claim.setEscalatedNow(true);
        }
        claim.setRemindedOn(today);
    }
}
