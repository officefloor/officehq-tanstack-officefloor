package net.officefloor.hq.app.period;

/** The fiscal year (yyyy) to generate the monthly periods of, and the signed-in user (`by`) generating them. */
public record GenerateFiscalYearRequest(String year, String by) {
}
