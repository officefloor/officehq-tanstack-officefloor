package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person as shown; grade is the one in force on the sheet's today (null before all their grades);
 *  grades every grade recorded, by from-date (null: from the beginning of time) for working out a claim's
 *  grade on its date; retiredFrom is yyyy-mm-dd, or null while not retired; held whether they have any hold;
 *  holds every hold, by from-date (null: from the beginning of time; to null: open-ended); patronIds the
 *  people named as their patrons. */
public record PersonResponse(long id, String name, String grade, long version, String retiredFrom,
        boolean held, List<Long> patronIds, List<GradeFrom> grades, List<HoldSpan> holds) {

    /** A grade held from a date on (yyyy-mm-dd; null: from the beginning of time). */
    public record GradeFrom(String from, String grade) {
    }

    /** A hold from a date to a date, both inclusive (yyyy-mm-dd; from null: the beginning of time; to null: open-ended). */
    public record HoldSpan(String from, String to) {
    }

    public static PersonResponse from(Person person, LocalDate today) {
        return new PersonResponse(person.getId(), person.getName(), person.gradeOn(today), person.getVersion(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(), person.isHeld(),
                person.getPatronIds().stream().sorted().toList(),
                person.getGrades().stream().map((g) -> new GradeFrom(
                        g.getFromDate() == null ? null : g.getFromDate().toString(), g.getGrade())).toList(),
                person.getHolds().stream().map((h) -> new HoldSpan(
                        h.getFromDate() == null ? null : h.getFromDate().toString(),
                        h.getToDate() == null ? null : h.getToDate().toString())).toList());
    }
}
