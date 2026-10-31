package net.officefloor.hq.app.person;

/** A person as the screen sees it. */
public record PersonResponse(long id, String name, String grade, int version) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion());
    }
}
