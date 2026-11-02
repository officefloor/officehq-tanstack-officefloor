package net.officefloor.hq.app.claim;

/** The body of a status change: the reason a reject gives, and the date (yyyy-MM-dd) a settle is as of, blank for
 *  the sheet's today as it stands (each ignored by the other changes). */
public record ClaimStatusRequest(String reason, String on) {
}
