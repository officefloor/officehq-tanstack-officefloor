package net.officefloor.hq.app.person;

/** The id of the person to name as a patron, and the signed-in user (`by`) doing it. */
public record PatronRequest(Long patron, String by) {
}
