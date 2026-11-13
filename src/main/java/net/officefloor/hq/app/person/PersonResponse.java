package net.officefloor.hq.app.person;

import java.util.List;

/**
 * A person as the screen shows them; the last statement's due-total and date are null before the first, and
 * {@code retiredFrom} is null while they are not retired; {@code held} is whether they are on hold. {@code grade} is
 * the grade in force on the sheet's today; {@code grades} every grade recorded, each from its from-date.
 */
public record PersonResponse(long id, String name, String grade, int version, int statementCount, String statedTotal,
        String statementDate, boolean retired, String retiredFrom, boolean held,
        List<Grade> grades) {

    /** A grade recorded for the person, from its from-date (yyyy-mm-dd; null: from the beginning of time). */
    public record Grade(String grade, String from) {
    }

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version(), person.statementCount(),
                person.statedTotal(), person.statementDate() == null ? null : person.statementDate().toString(),
                person.retired(), person.retiredFrom() == null ? null : person.retiredFrom().toString(),
                person.held(), person.grades().stream()
                        .map((g) -> new Grade(g.grade(), g.from() == null ? null : g.from().toString())).toList());
    }
}
