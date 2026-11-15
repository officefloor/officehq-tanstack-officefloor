package net.officefloor.hq.app.period;

/** Body of generating monthly periods: a from-date and a to-date (yyyy-MM-dd); a blank one is the sheet's today. */
public record GeneratePeriodsRequest(String from, String to) {
}
