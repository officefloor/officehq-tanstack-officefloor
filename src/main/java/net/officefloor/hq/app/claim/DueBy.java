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
        Integer own = person.getTerms();
        int terms = own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(date), date);
        return holidays.businessDaysAfter(person.getSheetId(), date, terms);
    }
}
