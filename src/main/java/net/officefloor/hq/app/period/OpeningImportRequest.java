package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods/openings: lines of name,period,amount. */
public record OpeningImportRequest(String text) {
}
