package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The due-by a new claim gets, fixed when it is created; a claim belongs to the period holding its due-by. */
public final class DueBy {

    private DueBy() {
    }

    /** The due-by of a new claim of the person dated on the date: the date plus the person's own terms (else the
     *  terms of their grade in force on the date (else the sheet's terms in force on it)), moved past the sheet's
     *  holidays as they are now. */
    public static LocalDate of(Person person, LocalDate date, SettingRepository settings, HolidayRepository holidays) {
        return holidays.nextWorkingDay(person.sheetId(), date.plusDays(person.terms() != null ? person.terms()
                : settings.termsFor(person.sheetId(), person.grades().on(date), date)));
    }
}
