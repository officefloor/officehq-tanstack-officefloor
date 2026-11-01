package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods/generate; a blank from-date is the sheet's today. */
public record GeneratePeriodsRequest(String from, String to) {
}
