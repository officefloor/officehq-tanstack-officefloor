package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/{name}/close (or /reopen): the user closing or reopening it. */
public record ClosePeriodRequest(String user) {
}
