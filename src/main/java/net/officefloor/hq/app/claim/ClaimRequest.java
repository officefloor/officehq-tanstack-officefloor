package net.officefloor.hq.app.claim;

/**
 * A claim's fields as typed: amount, date (blank for the sheet's today) and note, and the
 * signed-in user (`by`) making the change.
 */
public record ClaimRequest(String amount, String date, String note, String by) {
}
