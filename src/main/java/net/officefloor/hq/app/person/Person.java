package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;

/**
 * A person on a sheet. {@code grade} (one of A, B or C) is the first grade they were given, which applies
 * from the beginning of time; {@code dated} are the grades given since, each from its date on, by date.
 * {@code version} is raised on every edit. {@code retiredFrom} is the date they are retired from (null when
 * not retired); a person with one set is retired at once, whatever the date. {@code holds} are their holds, by
 * from-date: their OPEN claims dated within one, unless bound by a statement, count in no live total.
 * {@code terms} are their own terms in days, which come before those of their grade and of the sheet (null for none).
 */
public record Person(long id, String sheetId, String name, String grade, long version, LocalDate retiredFrom,
        List<Hold> holds, List<DatedGrade> dated, Integer terms) {

    public Person with(PersonRequest request) {
        return new Person(id, sheetId, request.name(), grade, version + 1, retiredFrom, holds, dated,
                request.terms() == null ? null : Integer.valueOf(request.terms()));
    }

    /** The grade in force on the date: the one with the latest from-date on or before it, else the first grade. */
    public String gradeOn(LocalDate date) {
        String on = grade;
        for (DatedGrade d : dated) {
            if (!d.from().isAfter(date)) {
                on = d.grade();
            }
        }
        return on;
    }

    /** The terms in force on the date: the person's own, else those of their grade then (else the sheet's). */
    public int termsOn(LocalDate date, Settings settings) {
        return terms != null ? terms : settings.terms(sheetId, gradeOn(date), date);
    }

    /** Whether the person has any hold. */
    public boolean held() {
        return !holds.isEmpty();
    }

    /** Whether the date falls within any of the person's holds. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }

    /** Whether the person's retirement date is reached on the date (it is on or after it). */
    public boolean retiredOn(LocalDate date) {
        return retiredFrom != null && date != null && !date.isBefore(retiredFrom);
    }

    public boolean retired() {
        return retiredFrom != null;
    }
}
