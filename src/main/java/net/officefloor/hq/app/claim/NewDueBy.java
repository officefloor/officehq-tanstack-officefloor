package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The due-by a claim of the person on the date is fixed with when it is created: the terms-th business day after its
 * date — skipping Saturdays, Sundays and the sheet's holidays (as the list stands now) — for the person's own terms,
 * else those of their grade, else the sheet's, each as in force on that date. */
public final class NewDueBy {

    private NewDueBy() {
    }

    public static LocalDate of(LocalDate date, Person person, SettingRepository settings,
            HolidayRepository holidays) {
        return holidays.businessDaysAfter(person.sheetId(), date,
                person.termsOn(settings.dated(person.sheetId()), date));
    }
}
