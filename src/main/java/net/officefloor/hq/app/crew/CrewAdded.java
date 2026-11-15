package net.officefloor.hq.app.crew;

/** A crew that went through, its lead's name, and who added it. */
public record CrewAdded(String sheetId, String name, String lead, String by) {
}
