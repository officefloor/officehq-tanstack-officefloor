package net.officefloor.hq.app.quick;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a quick-add while the due-by the claim would be fixed with (its date, the sheet's entry date
 * while it is set, else its today, its person's terms in force on the sheet's today in business days on, skipping weekends and holidays) is inside a closed period, before anything is written.
 */
public class CheckQuickAddPeriod {
    public void check(@Val Sheet sheet, @Val Person person, PeriodRepository periods, SettingsRepository settings,
            HolidayRepository holidays)
            throws ClosedPeriodException {
        Settings terms = settings.find(sheet.id());
        LocalDate date = terms.entryDay(sheet.today());
        Period closed = periods.closedOn(sheet.id(),
                holidays.dueBy(sheet.id(), date, terms.termsFor(person, sheet.today())));
        if (closed != null) throw new ClosedPeriodException(closed.name());
    }
}
