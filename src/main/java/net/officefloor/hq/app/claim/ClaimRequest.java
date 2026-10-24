package net.officefloor.hq.app.claim;

/** The claim form as submitted: an amount, a date (blank for the sheet's today), a note and the number of the
 * claim it corrects (blank for none). */
public record ClaimRequest(String amount, String date, String note, String corrects) {
}
