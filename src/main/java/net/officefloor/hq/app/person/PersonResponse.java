package net.officefloor.hq.app.person;

/** A person, as the screen reads it. */
public record PersonResponse(String id, String name, String grade) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade());
    }
}
