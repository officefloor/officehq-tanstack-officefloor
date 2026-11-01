package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods; a blank from-date is the sheet's today. */
public record PeriodRequest(String name, String from, String to) {
}
