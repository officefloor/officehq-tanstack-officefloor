package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;

/** The due-by a claim is fixed with when it is added; it also decides the claim's period. */
public final class DueBy {

    private DueBy() {
    }

    /** The due-by of a claim of `person` dated `date`: the terms-th business day after its date,
     *  taking the person's own terms, else the terms of their grade then, else the sheet's terms then,
     *  and skipping every Saturday, Sunday and holiday on the sheet now. */
    public static LocalDate of(Person person, LocalDate date, Settings settings, Holidays holidays) {
        return holidays.businessDaysAfter(person.getSheetId(), date, terms(person, date, settings));
    }

    /** The terms a claim of `person` dated `date` is fixed with: the person's own, else those of their
     *  grade then, else the sheet's then. */
    public static int terms(Person person, LocalDate date, Settings settings) {
        Integer own = person.getTerms();
        return own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(date), date);
    }
}
