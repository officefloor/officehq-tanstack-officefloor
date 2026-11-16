package net.officefloor.hq.app.period;

/** Body of generating periods: the from and to dates (yyyy-MM-dd, blank for the sheet's today), and who generates. */
public record GeneratePeriodsRequest(String from, String to, String by) {
}
