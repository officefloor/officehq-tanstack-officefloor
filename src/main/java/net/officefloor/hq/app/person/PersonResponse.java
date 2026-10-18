package net.officefloor.hq.app.person;

/** A person as shown; retiredFrom is yyyy-mm-dd, or null while not retired. */
public record PersonResponse(long id, String name, String grade, long version, String retiredFrom) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString());
    }
}
