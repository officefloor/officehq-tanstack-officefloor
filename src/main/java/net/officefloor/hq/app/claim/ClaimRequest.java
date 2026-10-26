package net.officefloor.hq.app.claim;

/**
 * The body of an add or an edit of a claim, as typed: the amount, the date (yyyy-mm-dd, blank for the sheet's today), the note
 * and, for a correction, the number of the claim it corrects (blank otherwise), and its external reference (blank for none).
 */
public record ClaimRequest(String amount, String date, String note, String corrects, String ref) {
}
