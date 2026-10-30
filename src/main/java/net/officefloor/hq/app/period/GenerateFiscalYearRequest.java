package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/generate-fy: the fiscal year (yyyy, named FY<yyyy> by the calendar
 *  year its first day falls in) whose twelve months each become a period. */
public record GenerateFiscalYearRequest(String year, String user) {
}
