package net.officefloor.hq.app.crew;

/** A member of a crew: the person and their name. */
public record CrewMember(long crewId, String sheetId, String crew, long personId, String name, String by) {
}
