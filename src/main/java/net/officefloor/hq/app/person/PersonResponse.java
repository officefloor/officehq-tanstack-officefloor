package net.officefloor.hq.app.person;

/** A person as the screen shows them; the last statement's due-total and date are null before the first. */
public record PersonResponse(long id, String name, String grade, int version, int statementCount, String statedTotal,
        String statementDate) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version(), person.statementCount(),
                person.statedTotal(), person.statementDate() == null ? null : person.statementDate().toString());
    }
}
