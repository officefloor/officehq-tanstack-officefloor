package net.officefloor.hq.app.period;

/** A fiscal year to generate monthly periods for, as the screen sends it: its name's year (yyyy). */
public record GenerateFiscalYearRequest(String year) {
}
