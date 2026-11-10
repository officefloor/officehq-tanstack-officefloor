package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person, as the screen reads it (their grade the one in force on the sheet's today), with their statement count, and the last statement's due-total
 * and date ("" before the first), whether they are retired and the date they are retired from
 * ("" when not), whether they are on hold, the ids of the people they name as patrons, and every
 * grade they have been given with the date it is from (the first "", from the beginning of time),
 * by from-date.
 */
public record PersonResponse(String id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, java.util.List<String> patronIds, List<GradeFrom> grades) {

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
                person.isHeld(), person.getPatronIds().stream().map(String::valueOf).toList(), grades);
    }
}
