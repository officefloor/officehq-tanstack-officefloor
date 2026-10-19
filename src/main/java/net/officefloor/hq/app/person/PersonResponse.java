package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person as the screen shows it, with the version an edit form sends back, the grade in force on the
 *  sheet's today and every dated grade (from: null for the beginning of time), and their statements:
 *  how many were taken, the last one's due-total (null: none taken, or n/a) and date (null: none taken),
 *  whether they are retired and from when (null: not retired), and whether they are on hold, and their patrons' names, sorted. */
public record PersonResponse(long id, String name, String grade, List<GradeResponse> grades, long version,
        int statementCount, String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<String> patrons) {

    public record GradeResponse(String grade, String from) {
    }

    public static PersonResponse from(Person person, LocalDate today) {
        return new PersonResponse(person.getId(), person.getName(), person.gradeOn(today),
                person.getGrades().stream()
                        .map(g -> new GradeResponse(g.grade(), g.from() == null ? null : g.from().toString())).toList(),
                person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() == null ? null : person.getStatedTotal().toPlainString(),
                person.getStatementDate() == null ? null : person.getStatementDate().toString(),
                person.getRetiredFrom() != null,
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(),
                person.isHeld(), List.copyOf(person.getPatrons()));
    }
}
