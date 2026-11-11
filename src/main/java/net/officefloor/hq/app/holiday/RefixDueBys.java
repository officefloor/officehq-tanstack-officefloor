package net.officefloor.hq.app.holiday;

import java.util.Set;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

/**
 * Fixes again the due-by of every claim on the sheet that is not SETTLED or VOID and is due on or after
 * the new holiday, unless a steward set its due-by directly: from its date, with the terms it was fixed with, over the holidays on the sheet now.
 */
public class RefixDueBys {
    public void refix(@Val Holiday holiday, HolidayRepository holidays, ClaimRepository claims) {
        Set<LocalDate> dates = holidays.dates(holiday.sheetId());
        for (Claim claim : claims.findAll(holiday.sheetId())) {
            if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) continue;
            if (claim.isDueBySet() || claim.getDueBy().isBefore(holiday.date())) continue;
            LocalDate was = claim.getDueBy();
            claim.fixDueBy(claim.getTerms(), dates);
            if (!claim.getDueBy().equals(was)) claims.saveDueBy(claim);
        }
    }
}
