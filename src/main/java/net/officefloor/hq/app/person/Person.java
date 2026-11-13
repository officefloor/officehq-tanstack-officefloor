package net.officefloor.hq.app.person;

/** A person on a sheet. {@code id} is null until the person is first saved; {@code version} counts its saves. */
public record Person(Long id, String sheetId, String name, String grade, int version) {
}
