package net.officefloor.hq.app.claim;

/** The claim form: the request body to add or edit a claim. A blank date means the sheet's today. */
public record ClaimRequest(String amount, String date, String note) {
}
