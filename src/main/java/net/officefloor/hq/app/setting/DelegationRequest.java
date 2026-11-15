package net.officefloor.hq.app.setting;

/** Body of delegating the steward role: the user, and the from-date and to-date (blank: the sheet's today). */
public record DelegationRequest(String user, String from, String to) {
}
