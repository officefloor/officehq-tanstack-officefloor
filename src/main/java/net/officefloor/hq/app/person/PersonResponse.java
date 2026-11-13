package net.officefloor.hq.app.person;

public record PersonResponse(long id, String name, String grade, int version) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version());
    }
}
