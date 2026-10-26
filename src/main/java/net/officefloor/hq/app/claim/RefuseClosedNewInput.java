package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseClosedNewInput {
    // A claim belongs to the period its due-by falls in: the due-by the claim being added would be fixed with (its date
    // — a blank date is the entry date, else the sheet's today — plus its person's terms, moved past the sheet's holidays) must not be in a closed period.
    public void refuse(@PathVariable("sheetId") String sheetId, @Val ClaimInput input, @Val Person person,
            Settings settings, Holidays holidays, Periods periods) throws ClaimInClosedPeriodException {
        String sheet = sheetId.trim();
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(sheet);
        check(periods, sheet, holidays.dueBy(sheet, date, person.termsOn(date, settings)));
    }

    /** Refuses a new claim due by {@code dueBy} when that falls inside a closed period. */
    static void check(Periods periods, String sheet, LocalDate dueBy) throws ClaimInClosedPeriodException {
        Period closed = periods.closedOn(sheet, dueBy);
        if (closed != null) {
            throw new ClaimInClosedPeriodException("The period " + closed.name() + " is closed: no claim due by "
                    + dueBy + " can be added.");
        }
    }
}
