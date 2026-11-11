package net.officefloor.hq.app.crew;

/** A member of a crew: a person on the sheet other than its lead. */
public record CrewMember(String sheetId, String crew, Long personId, String name) {
}
