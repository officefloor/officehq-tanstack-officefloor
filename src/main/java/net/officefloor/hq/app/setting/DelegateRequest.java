package net.officefloor.hq.app.setting;

/** The body of a delegation: the user name, and the from- and to-dates as typed (yyyy-mm-dd). */
public record DelegateRequest(String user, String from, String to) {
}
