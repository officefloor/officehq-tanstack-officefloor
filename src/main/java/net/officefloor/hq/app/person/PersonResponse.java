package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person as the screen sees it, with their statement count, and the last statement's due-total
 *  (two decimals, null when it was n/a or none was taken) and date (yyyy-mm-dd, null when none was),
 *  and whether they are retired and from when (yyyy-mm-dd, null when not retired),
 *  and whether they are on hold, and the ids of the people they have named as patrons. `grade` is
 *  the grade in force on the sheet's today; `grades` every grade recorded, by from-date (null: from
 *  the beginning of time, first). */
public record PersonResponse(long id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<Long> patrons, List<GradeFrom> grades) {

    public record GradeFrom(String grade, String from) {
    }

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() != null ? person.getStatedTotal().toPlainString() : null,
                person.getStatementDate() != null ? person.getStatementDate().toString() : null,
                person.isRetired(), person.getRetiredFrom() != null ? person.getRetiredFrom().toString() : null,
                person.isHeld(), List.copyOf(person.getPatronIds()),
                person.getGrades().entrySet().stream()
                        .map(e -> new GradeFrom(e.getValue(), e.getKey().equals(LocalDate.MIN) ? null : e.getKey().toString()))
                        .toList());
    }
}
