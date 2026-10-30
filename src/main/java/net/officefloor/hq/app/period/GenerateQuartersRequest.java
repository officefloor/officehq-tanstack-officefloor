package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/generate-quarters: the calendar year (yyyy) whose four quarters
 *  each become a period. */
public record GenerateQuartersRequest(String year, String user) {
}
