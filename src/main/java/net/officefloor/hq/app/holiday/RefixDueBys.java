package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.plugin.variable.Val;

/** Runs after the holiday is saved: every claim on the sheet that is not SETTLED or VOID and is due by
 *  the new holiday or later has its due-by worked out again from its date, with the terms it was
 *  fixed with, over the holidays now on the sheet. */
public class RefixDueBys {
    public void refix(@Val Holiday holiday, Claims claims, Holidays holidays) {
        for (Claim claim : claims.onSheet(holiday.sheetId())) {
            if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) continue;
            if (claim.getDueBy().isBefore(holiday.date()) || claim.getDueTerms() == null) continue;
            LocalDate dueBy = holidays.businessDaysAfter(holiday.sheetId(), claim.getDate(), claim.getDueTerms());
            if (dueBy.equals(claim.getDueBy())) continue;
            claim.setDueBy(dueBy);
            claims.redue(claim);
        }
    }
}
