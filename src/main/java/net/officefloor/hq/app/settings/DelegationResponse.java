package net.officefloor.hq.app.settings;

/** A delegation as the screen sees it: the user, the from-date and to-date (yyyy-mm-dd) and the
 *  steward who delegated. */
public record DelegationResponse(String name, String from, String to, String by) {
}
