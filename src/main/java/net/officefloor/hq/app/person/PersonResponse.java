package net.officefloor.hq.app.person;

public record PersonResponse(long id, String name, String grade) {
    public static PersonResponse from(Person p) {
        return new PersonResponse(p.id(), p.name(), p.grade());
    }
}
