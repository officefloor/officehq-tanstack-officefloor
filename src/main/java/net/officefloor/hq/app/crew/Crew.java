package net.officefloor.hq.app.crew;

/** A crew of the sheet: its name, and its lead's id and name. */
public record Crew(String sheetId, String name, long leadId, String lead) {
}
