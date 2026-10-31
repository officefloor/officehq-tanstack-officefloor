package net.officefloor.hq.app.claim;

/** Body of a claim add or edit, as typed: the amount, the date (blank for the sheet's today),
 *  the note and who makes the change. */
public record ClaimRequest(String amount, String date, String note, String user) {
}
