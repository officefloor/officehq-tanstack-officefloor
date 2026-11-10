package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person, as the screen reads it (their grade the one in force on the sheet's today), with their statement count, and the last statement's due-total
 * and date ("" before the first), whether they are retired and the date they are retired from
 * ("" when not), whether they have any hold, their holds (in the order added), the ids of the people they name as patrons, and every
 * grade they have been given with the date it is from (the first "", from the beginning of time),
 * by from-date, their own terms in days ("" for none), and their contact ("" for none).
 */
public record PersonResponse(String id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<HoldFrom> holds, java.util.List<String> patronIds, List<GradeFrom> grades, String terms,
        String contact) {

    /** A hold from a date to a date (yyyy-MM-dd; to "" when open-ended), both inclusive. */
    public record HoldFrom(String from, String to) {}

    /** A grade in force from a date (yyyy-MM-dd; "" from the beginning of time). */
    public record GradeFrom(String from, String grade) {}

    /** The person as the screen reads it on the sheet's today: their grade is the one in force on it. */
    public static PersonResponse from(Person person, LocalDate today) {
        List<GradeFrom> grades = new ArrayList<>();
        grades.add(new GradeFrom("", person.getGrade()));
        person.getDatedGrades().forEach((from, grade) -> grades.add(new GradeFrom(from.toString(), grade)));
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.gradeOn(today),
                person.getVersion(), person.getStatementCount(),
                person.getStatedTotal() == null ? "" : person.getStatedTotal(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString(),
                person.isRetired(), person.getRetiredFrom() == null ? "" : person.getRetiredFrom().toString(),
                person.isHeld(),
                person.getHolds().stream().map((h) -> new HoldFrom(h.from().toString(), h.to() == null ? "" : h.to().toString())).toList(),
                person.getPatronIds().stream().map(String::valueOf).toList(), grades,
                person.getTerms() == null ? "" : person.getTerms().toString(),
                person.getContact() == null ? "" : person.getContact());
    }
}
