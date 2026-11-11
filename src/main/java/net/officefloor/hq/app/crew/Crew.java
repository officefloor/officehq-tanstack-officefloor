package net.officefloor.hq.app.crew;

/** A crew on a sheet: a trimmed name, at most once per sheet, and its lead (a person on the sheet). */
public record Crew(Long id, String sheetId, String name, Long leadId, String lead) {
}
