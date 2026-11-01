package net.officefloor.hq.app.crew;

/** A crew on a sheet with its lead (a person of the sheet, `leadName` theirs), added by `by`. */
public record Crew(String sheetId, String name, long leadId, String leadName, String by) {
}
