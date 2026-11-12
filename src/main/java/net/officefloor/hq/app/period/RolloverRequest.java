package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/rollover: the fiscal year to roll over (yyyy, as FY<yyyy>) and who
 *  rolls it over. */
public record RolloverRequest(String year, String user) {
}
