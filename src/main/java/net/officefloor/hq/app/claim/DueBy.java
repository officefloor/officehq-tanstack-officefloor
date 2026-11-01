package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;

/** The due-by a claim is fixed with when it is added; it also decides the claim's period. */
public final class DueBy {

    private DueBy() {
    }

    /** The due-by of a claim of `person` dated `date` created now: the terms-th business day after its
     *  date, taking the terms in force on the sheet's today (see `terms`), and skipping every Saturday,
     *  Sunday and holiday on the sheet now. */
    public static LocalDate of(Person person, LocalDate date, Settings settings, Holidays holidays) {
        return holidays.businessDaysAfter(person.getSheetId(), date, terms(person, settings));
    }

    /** The terms a claim of `person` created now is fixed with, each looked up on the day it is created
     *  (the sheet's today), not on its date: the person's own in force then, else those of the grade
     *  they had then, else the sheet's then. */
    public static int terms(Person person, Settings settings) {
        LocalDate created = settings.today(person.getSheetId());
        Integer own = person.termsOn(created);
        return own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(created), created);
    }
}
