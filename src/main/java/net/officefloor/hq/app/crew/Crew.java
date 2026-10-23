package net.officefloor.hq.app.crew;

/** A crew on a sheet: its name (trimmed, at most once on a sheet), its lead's person id, and who added it. */
public record Crew(long id, String sheetId, String name, long leadId, String by) {
}
