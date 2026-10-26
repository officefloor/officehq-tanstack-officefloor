package net.officefloor.hq.app.person;

import java.time.LocalDate;

/**
 * A person on a sheet. {@code grade} is one of A, B or C; {@code version} is raised on every edit.
 * {@code retiredFrom} is the date they are retired from (null when not retired); a person with one set
 * is retired at once, whatever the date. {@code held} is whether they are on hold: while held, their OPEN
 * claims count in no live total.
 */
public record Person(long id, String sheetId, String name, String grade, long version, LocalDate retiredFrom,
        boolean held) {

    public Person with(PersonRequest request) {
        return new Person(id, sheetId, request.name(), request.grade(), version + 1, retiredFrom, held);
    }

    public boolean retired() {
        return retiredFrom != null;
    }
}
