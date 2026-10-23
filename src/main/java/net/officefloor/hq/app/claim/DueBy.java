package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The terms a new claim is fixed with when it is created, and the due-by they give; a claim belongs to the period
 *  holding its due-by. */
public final class DueBy {

    private DueBy() {
    }

    /** The terms (business days) a new claim of the person dated on the date is fixed with: the person's own terms
     *  in force on the date, else the terms of their grade in force on it (else the sheet's terms in force on it). */
    public static int terms(Person person, LocalDate date, SettingRepository settings) {
        Integer own = person.terms().on(date);
        return own != null ? own : settings.termsFor(person.sheetId(), person.grades().on(date), date);
    }

    /** The due-by of a new claim of the person dated on the date: the terms-th business day after the date, skipping
     *  Saturdays, Sundays and the sheet's holidays as they are now. */
    public static LocalDate of(Person person, LocalDate date, SettingRepository settings, HolidayRepository holidays) {
        return holidays.dueBy(person.sheetId(), date, terms(person, date, settings));
    }
}
