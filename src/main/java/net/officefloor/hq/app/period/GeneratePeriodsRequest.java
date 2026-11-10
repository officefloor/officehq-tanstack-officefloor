package net.officefloor.hq.app.period;

/** Monthly periods to generate, as the screen sends them: a from and to date (yyyy-MM-dd; a blank from is the sheet's today). */
public record GeneratePeriodsRequest(String from, String to) {
}
