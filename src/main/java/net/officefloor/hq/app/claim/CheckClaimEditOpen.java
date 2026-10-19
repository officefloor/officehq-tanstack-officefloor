package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

/** Refuses editing a claim dated inside a closed period, or moving one into a closed period. */
public class CheckClaimEditOpen {
    public void check(@Val ValidClaim request, @Val Claim claim, SheetClock clock, PeriodRepository periods)
            throws InvalidClaimException {
        ClosedPeriods.refuse(periods, claim.getSheetId(), claim.getDate());
        LocalDate date = request.date() != null ? request.date() : clock.today(claim.getSheetId());
        ClosedPeriods.refuse(periods, claim.getSheetId(), date);
    }
}
