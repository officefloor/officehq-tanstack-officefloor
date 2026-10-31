package net.officefloor.hq.app.period;

/** Body of a generate: the from and to dates (yyyy-mm-dd; a blank one is the sheet's today) whose
 *  months get a period each, and who generates them. */
public record GeneratePeriodsRequest(String from, String to, String user) {
}
