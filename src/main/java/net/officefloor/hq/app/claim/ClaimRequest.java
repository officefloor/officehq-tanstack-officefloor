package net.officefloor.hq.app.claim;

/**
 * A claim's fields as typed: amount, date (blank for the sheet's today), note and the number of the
 * claim a correction corrects (blank for none), and the signed-in user (`by`) making the change.
 */
public record ClaimRequest(String amount, String date, String note, String corrects, String by) {
}
