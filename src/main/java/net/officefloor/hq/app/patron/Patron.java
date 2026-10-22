package net.officefloor.hq.app.patron;

/** A person's patron: another person on the same sheet, by id, with the patron's name. */
public record Patron(String sheetId, long personId, long patronId, String patronName) {
}
