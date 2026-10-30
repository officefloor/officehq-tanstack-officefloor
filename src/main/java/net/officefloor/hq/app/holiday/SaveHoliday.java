package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Adds the holiday and re-fixes the due-by of every claim not SETTLED, VOID or DELETED whose due-by is on or after it: worked
 * out again from the claim's date, with the terms it was fixed with, over the holidays now on the sheet.
 */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidHoliday holiday, @Val RollupFigures before,
            Holidays holidays, Claims claims, Rollup rollup, Audit audit, ObjectResponse<List<String>> response) {
        holidays.add(sheetId, holiday.date());
        for (Claim claim : claims.list(sheetId)) {
            if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID
                    || claim.getStatus() == ClaimStatus.DELETED
                    || claim.getDueBy() == null || claim.getTerms() == null || claim.getDueBy().isBefore(holiday.date())) {
                continue;
            }
            java.time.LocalDate dueBy = holidays.dueBy(sheetId, claim.getDate(), claim.getTerms());
            if (!dueBy.equals(claim.getDueBy())) {
                claim.setDueBy(dueBy);
                claims.refix(claim);
            }
        }
        audit.recordOnCommit("HOLIDAY_ADDED sheet=" + sheetId + " by=" + holiday.user() + " date=" + holiday.date());
        rollup.recordChanges(sheetId, holiday.user(), before);
        response.send(holidays.list(sheetId));
    }
}
