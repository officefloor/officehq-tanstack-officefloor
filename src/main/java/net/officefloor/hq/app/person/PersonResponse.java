package net.officefloor.hq.app.person;

/**
 * A person as the screen shows them; the last statement's due-total and date are null before the first, and
 * {@code retiredFrom} is null while they are not retired; {@code held} is whether they are on hold.
 */
public record PersonResponse(long id, String name, String grade, int version, int statementCount, String statedTotal,
        String statementDate, boolean retired, String retiredFrom, boolean held) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version(), person.statementCount(),
                person.statedTotal(), person.statementDate() == null ? null : person.statementDate().toString(),
                person.retired(), person.retiredFrom() == null ? null : person.retiredFrom().toString(),
                person.held());
    }
}
