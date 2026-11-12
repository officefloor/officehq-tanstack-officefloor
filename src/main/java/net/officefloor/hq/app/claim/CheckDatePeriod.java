package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim added for the person when the due-by it would be fixed with (its date plus the terms it
 *  would get) falls inside a closed period. */
public class CheckDatePeriod {
    public void check(@Val ClaimInput input, @Val Person person, SettingStore settings, PeriodStore periods,
            HolidayStore holidays)
            throws InvalidSheetException {
        ClaimPeriods.refuseClosed(periods, person.getSheetId(),
                ClaimPeriods.dueByOf(person, input.date(), settings, holidays));
    }
}
