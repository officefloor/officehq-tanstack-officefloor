package net.officefloor.hq.app.claim;

/** The body of an add or an edit of a claim, as typed: the amount, the date (yyyy-mm-dd, blank for the sheet's today) and the note. */
public record ClaimRequest(String amount, String date, String note) {
}
