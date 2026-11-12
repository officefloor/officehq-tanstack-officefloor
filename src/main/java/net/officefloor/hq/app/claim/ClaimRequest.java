package net.officefloor.hq.app.claim;

/** Body of a claim add or edit, as typed: the amount, the date (blank for the sheet's today), the
 *  note, who makes the change, and the number of the claim a correction corrects (blank for none). */
public record ClaimRequest(String amount, String date, String note, String user, String corrects) {
}
