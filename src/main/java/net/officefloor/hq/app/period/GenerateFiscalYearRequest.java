package net.officefloor.hq.app.period;

/** Body of a fiscal year generate: the fiscal year (yyyy, the calendar year its first day falls in)
 *  whose twelve months get a period each, and who generates them. */
public record GenerateFiscalYearRequest(String year, String user) {
}
