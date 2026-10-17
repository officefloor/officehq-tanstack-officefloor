package net.officefloor.hq.app.claim;

/** The body of adding or editing a claim: an amount (required), a date (empty = the sheet's today) and a note. */
public record ClaimRequest(String amount, String date, String note) {
}
