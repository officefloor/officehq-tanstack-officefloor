package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/generate-quarters: the calendar year (yyyy) and who generates
 *  its quarters. */
public record GenerateQuartersRequest(String year, String user) {
}
