package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;

/** The due-by a claim is fixed with when it is added; it also decides the claim's period. */
public final class DueBy {

    private DueBy() {
    }

    /** The due-by of a claim of `person` dated `date`: its date plus the person's own terms, else
     *  the terms of their grade then, else the sheet's terms then; moved, when that is a holiday on
     *  the sheet now, to the next day that is not one. */
    public static LocalDate of(Person person, LocalDate date, Settings settings, Holidays holidays) {
        Integer own = person.getTerms();
        LocalDate due = date.plusDays(own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(date), date));
        return holidays.nextWorking(person.getSheetId(), due);
    }
}
