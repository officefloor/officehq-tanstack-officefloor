package net.officefloor.hq.app.person;

/** A person on a sheet. {@code grade} is one of A, B or C; {@code version} is raised on every edit. */
public record Person(long id, String sheetId, String name, String grade, long version) {

    public Person with(PersonRequest request) {
        return new Person(id, sheetId, request.name(), request.grade(), version + 1);
    }
}
