package net.officefloor.hq.app.person;

/** A person as the screen sees it. {@code retiredFrom} is yyyy-mm-dd, null when not retired. */
public record PersonResponse(long id, String name, String grade, long version, String retiredFrom) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version(),
                person.retiredFrom() == null ? null : person.retiredFrom().toString());
    }
}
