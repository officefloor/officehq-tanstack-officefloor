package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/{name}/close: the user closing it. */
public record ClosePeriodRequest(String user) {
}
