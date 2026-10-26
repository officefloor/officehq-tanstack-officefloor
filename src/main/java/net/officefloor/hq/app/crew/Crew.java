package net.officefloor.hq.app.crew;

/** A crew of a sheet to add: its name (trimmed) and its lead (a person's id). */
public record Crew(String sheetId, String name, long leadId) {
}
