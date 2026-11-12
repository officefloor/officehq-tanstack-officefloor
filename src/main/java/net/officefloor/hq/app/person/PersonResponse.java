package net.officefloor.hq.app.person;

/** A person as the front end reads it. */
public record PersonResponse(long id, String name, String grade, long version) {
    public static PersonResponse from(Person p) {
        return new PersonResponse(p.getId(), p.getName(), p.getGrade(), p.getVersion());
    }
}
