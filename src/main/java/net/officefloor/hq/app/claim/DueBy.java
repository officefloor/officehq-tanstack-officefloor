package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The terms a new claim is fixed with when it is created, and the due-by they give; a claim belongs to the period
 *  holding its due-by. */
public final class DueBy {

    private DueBy() {
    }

    /** The terms (business days) a new claim of the person dated on the date is fixed with: those in force on the day
     *  it is created (the sheet's today) — the person's own terms in force on that day, else the terms in force on it
     *  of their grade on the claim's date (else the sheet's terms in force on it). */
    public static int terms(Person person, LocalDate date, SettingRepository settings, SheetRepository sheets) {
        var today = sheets.today(person.sheetId());
        Integer own = person.terms().on(today);
        return own != null ? own : settings.termsFor(person.sheetId(), person.grades().on(date), today);
    }

    /** The due-by of a new claim of the person dated on the date: the terms-th business day after the date, skipping
     *  Saturdays, Sundays and the sheet's holidays as they are now. */
    public static LocalDate of(Person person, LocalDate date, SettingRepository settings, SheetRepository sheets,
            HolidayRepository holidays) {
        return holidays.dueBy(person.sheetId(), date, terms(person, date, settings, sheets));
    }
}
