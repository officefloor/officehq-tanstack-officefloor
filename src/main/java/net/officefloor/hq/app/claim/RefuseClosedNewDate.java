package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a new claim (date blank: the sheet's entry date, else its today) whose due-by, its date plus the terms it would
 * get (those in force on the sheet's today), falls inside a closed period.
 */
public class RefuseClosedNewDate {

    public void check(@Val Person person, @Val ClaimInput input, SheetRepository sheets,
            SettingRepository settings, PeriodRepository periods, HolidayRepository holidays) throws BadRequestException {
        LocalDate today = sheets.today(person.getSheetId());
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(person.getSheetId(), today);
        RefuseClosedDate.refuse(person.getSheetId(), BuildClaim.dueBy(person, date, today, settings, holidays), periods);
    }
}
