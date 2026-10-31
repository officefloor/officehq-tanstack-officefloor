package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/rollover: the fiscal year (yyyy) to roll over into the next. */
public record RolloverRequest(String year, String user) {
}
