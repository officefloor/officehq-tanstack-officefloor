package net.officefloor.hq.app.crew;

/** A member of a crew: the crew's name, and the person's id and name. */
public record CrewMember(String sheetId, String crew, long personId, String name) {
}
