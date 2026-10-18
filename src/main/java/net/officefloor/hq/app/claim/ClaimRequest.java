package net.officefloor.hq.app.claim;

/** The body of adding or editing a claim: an amount (required), a date (empty = the sheet's today), a note and, for a correction
 *  (a negative amount), the number of the settled claim it corrects. */
public record ClaimRequest(String amount, String date, String note, String corrects) {
}
