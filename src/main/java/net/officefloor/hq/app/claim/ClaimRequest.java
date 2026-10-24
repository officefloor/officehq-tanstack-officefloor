package net.officefloor.hq.app.claim;

/** The claim form as submitted: an amount, a date (blank for the sheet's today) and a note. */
public record ClaimRequest(String amount, String date, String note) {
}
