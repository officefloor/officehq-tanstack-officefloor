package net.officefloor.hq.app.crew;

/** The member just added: the crew's name and the person's name. */
public record CrewMemberAdded(String sheetId, String crew, String name) {
}
