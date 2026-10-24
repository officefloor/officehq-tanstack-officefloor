package net.officefloor.hq.app.period;

/** The generate-periods form as submitted: a from and a to date (YYYY-MM-DD), either blank for the sheet's today. */
public record GeneratePeriodsRequest(String from, String to) {
}
