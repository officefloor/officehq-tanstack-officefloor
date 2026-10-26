package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/**
 * A person on a sheet. {@code grade} (one of A, B or C) is the first grade they were given, which applies
 * from the beginning of time; {@code dated} are the grades given since, each from its date on, by date.
 * {@code version} is raised on every edit. {@code retiredFrom} is the date they are retired from (null when
 * not retired); a person with one set is retired at once, whatever the date. {@code holds} are their holds, by
 * from-date: their OPEN claims dated within one, unless bound by a statement, count in no live total.
 */
public record Person(long id, String sheetId, String name, String grade, long version, LocalDate retiredFrom,
        List<Hold> holds, List<DatedGrade> dated) {

    public Person with(PersonRequest request) {
        return new Person(id, sheetId, request.name(), grade, version + 1, retiredFrom, holds, dated);
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

    /** Whether the person has any hold. */
    public boolean held() {
        return !holds.isEmpty();
    }

    /** Whether the date falls within any of the person's holds. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }

    public boolean retired() {
        return retiredFrom != null;
    }
}
