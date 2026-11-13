package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The due-by a claim of the person on the date is fixed with when it is created: its date plus the person's own
 * terms, else those of their grade, else the sheet's, each as in force on that date; when that falls on one of the
 * sheet's holidays (as the list stands now), the next day that is not one. */
public final class NewDueBy {

    private NewDueBy() {
    }

    public static LocalDate of(LocalDate date, Person person, SettingRepository settings,
            HolidayRepository holidays) {
        return holidays.nextWorkingDay(person.sheetId(),
                date.plusDays(person.termsOn(settings.dated(person.sheetId()), date)));
    }
}
