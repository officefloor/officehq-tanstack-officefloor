package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim, once per sheet-today: its reminded date becomes the sheet's today. While the sheet
 *  has an escalation gap, a reminder whose previous one was at least that many days before escalates the claim, once ever. */
public class ApplyRemind {
    public void apply(@Val RemindRequest request, @Val Claim claim, Sheets sheets, Settings settings) throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new ClaimStatusRefusedException("A claim can be reminded only once per sheet-today");
        }
        Integer gap = settings.escalateAfter(claim.getSheetId());
        LocalDate previous = claim.getRemindedOn();
        if (gap != null && previous != null && !claim.isEscalated() && ChronoUnit.DAYS.between(previous, today) >= gap) {
            claim.setEscalated(true);
            claim.setEscalating(true);
        }
        claim.setRemindedOn(today);
    }
}
