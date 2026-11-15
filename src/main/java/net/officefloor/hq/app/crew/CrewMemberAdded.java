package net.officefloor.hq.app.crew;

/** A member added to a crew that went through, and who added them. */
public record CrewMemberAdded(String sheetId, String crew, String name, String by) {
}
