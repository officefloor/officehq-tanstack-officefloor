package net.officefloor.hq.app.person;

/** A person on a sheet. {@code grade} is one of A, B or C. */
public record Person(long id, String sheetId, String name, String grade) {

    public Person with(PersonRequest request) {
        return new Person(id, sheetId, request.name(), request.grade());
    }
}
