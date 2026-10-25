package net.officefloor.hq.app.person;

public record PersonResponse(long id, String name, String grade) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade());
    }
}
