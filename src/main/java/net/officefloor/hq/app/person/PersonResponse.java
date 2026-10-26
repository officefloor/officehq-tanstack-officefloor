package net.officefloor.hq.app.person;

/** A person as the screen sees it. */
public record PersonResponse(long id, String name, String grade, long version) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version());
    }
}
