package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;

/** The due-by a claim is fixed with when it is added; it also decides the claim's period. */
public final class DueBy {

    private DueBy() {
    }

    /** The due-by of a claim of `person` dated `date`: its date plus the person's own terms, else
     *  the terms of their grade then, else the sheet's terms then. */
    public static LocalDate of(Person person, LocalDate date, Settings settings) {
        Integer own = person.getTerms();
        return date.plusDays(own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(date), date));
    }
}
