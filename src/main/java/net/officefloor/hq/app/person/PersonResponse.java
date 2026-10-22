package net.officefloor.hq.app.person;

public record PersonResponse(long id, String name, String grade, long version) {
    public static PersonResponse from(Person p) {
        return new PersonResponse(p.id(), p.name(), p.grade(), p.version());
    }
}
