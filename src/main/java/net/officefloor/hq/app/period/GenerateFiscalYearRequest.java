package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods/generate-fy: the fiscal year, by the calendar year its first day falls in. */
public record GenerateFiscalYearRequest(String year) {
}
