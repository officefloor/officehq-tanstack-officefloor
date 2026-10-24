package net.officefloor.hq.app.period;

/** The generate-fiscal-year form as submitted: the fiscal year (yyyy), named by the calendar year its first day falls in. */
public record GenerateFiscalYearRequest(String year) {
}
