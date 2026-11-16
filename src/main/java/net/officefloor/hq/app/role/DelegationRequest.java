package net.officefloor.hq.app.role;

/** Body of delegating the steward role: the user, the from- and to-date (yyyy-MM-dd; a blank from-date is the sheet's
 *  today, a blank to-date the from-date), and the steward who delegates. */
public record DelegationRequest(String user, String from, String to, String by) {
}
