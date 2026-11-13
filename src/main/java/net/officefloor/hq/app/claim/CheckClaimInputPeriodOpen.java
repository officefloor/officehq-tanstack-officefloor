package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses (400) a claim about to be added whose due-by — its date (blank: the sheet's entry date, else its today)
 * plus the terms it would get (see {@link NewDueBy}) — falls inside a closed period. */
public class CheckClaimInputPeriodOpen {
    public void check(@Val ClaimInput input, @Val Person person, @PathVariable("sheetId") String sheetId,
            SheetRepository sheets, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods)
            throws InvalidClaimException {
        LocalDate date = input.date() != null ? input.date() : sheets.entryDate(sheetId);
        LocalDate dueBy = NewDueBy.of(date, person, settings, holidays);
        Period closed = periods.closedOn(sheetId, dueBy);
        if (closed != null) {
            throw new InvalidClaimException("A claim dated " + date + " is due by " + dueBy
                    + ", in the closed period " + closed.name() + ".");
        }
    }
}
