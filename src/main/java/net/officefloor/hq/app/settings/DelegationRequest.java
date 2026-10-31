package net.officefloor.hq.app.settings;

/** Body of a delegation: the user given the steward role, its from-date and to-date (yyyy-mm-dd,
 *  inclusive; a blank from-date is the sheet's today, a blank to-date the from-date) and who delegates. */
public record DelegationRequest(String name, String from, String to, String user) {
}
