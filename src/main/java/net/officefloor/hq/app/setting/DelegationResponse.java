package net.officefloor.hq.app.setting;

/** A delegation of the steward role as shown: the user, the from-date and to-date, and the steward who delegated. */
public record DelegationResponse(String user, String from, String to, String by) {
}
