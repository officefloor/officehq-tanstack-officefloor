package net.officefloor.hq.app.period;

/** Body of generating a fiscal year's periods: the year (yyyy, the calendar year its first day falls in), and who
 *  generates. */
public record GenerateFiscalYearRequest(String year, String by) {
}
