package net.officefloor.hq.app.claim;

/** The new date (yyyy-mm-dd) of an OPEN or PENDING claim, and the signed-in user (`by`) re-dating it. */
public record RedateRequest(String date, String by) {
}
