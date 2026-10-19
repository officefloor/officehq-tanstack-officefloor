package net.officefloor.hq.app.person;

/** A person as the screen shows it, with the version an edit form sends back. */
public record PersonResponse(long id, String name, String grade, long version) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion());
    }
}
