package net.officefloor.hq.app.person;

import java.util.List;

/** A person as the screen shows it, with the version an edit form sends back, and their statements:
 *  how many were taken, the last one's due-total (null: none taken, or n/a) and date (null: none taken),
 *  whether they are retired and from when (null: not retired), and whether they are on hold, and their patrons' names, sorted. */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<String> patrons) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() == null ? null : person.getStatedTotal().toPlainString(),
                person.getStatementDate() == null ? null : person.getStatementDate().toString(),
                person.getRetiredFrom() != null,
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(),
                person.isHeld(), List.copyOf(person.getPatrons()));
    }
}
