package net.officefloor.hq.app.period;

/** Body of a fiscal year generate: who is making it, and the fiscal year (yyyy: FY<yyyy> starts in that year). */
public record FiscalYearGenerateRequest(String user, String year) {
}
