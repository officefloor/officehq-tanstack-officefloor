package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

/** Refuses a new claim dated (or, without a date, falling on the sheet's today) after its person's retirement. */
public class CheckClaimAddRetired {
    public void check(@Val ValidClaim request, @Val Person person, SheetClock clock) throws InvalidClaimException {
        LocalDate date = request.date() != null ? request.date() : clock.today(person.getSheetId());
        Retired.refuse(person, date);
    }
}
