package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

/** Refuses a new claim dated (or, without a date, falling on the sheet's today) inside a closed period. */
public class CheckClaimAddOpen {
    public void check(@Val ValidClaim request, @Val Person person, SheetClock clock, PeriodRepository periods)
            throws InvalidClaimException {
        LocalDate date = request.date() != null ? request.date() : clock.today(person.getSheetId());
        ClosedPeriods.refuse(periods, person.getSheetId(), date);
    }
}
