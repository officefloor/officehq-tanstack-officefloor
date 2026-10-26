package net.officefloor.hq.app.crew;

/** The crew just added: its name and its lead's name. */
public record CrewAdded(String sheetId, String name, String lead) {
}
