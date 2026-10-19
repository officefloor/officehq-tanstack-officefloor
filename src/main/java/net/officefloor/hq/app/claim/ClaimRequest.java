package net.officefloor.hq.app.claim;

/** The claim form: the request body to add or edit a claim. A blank date means the sheet's today.
 *  corrects: the number of the SETTLED claim a correction (a negative amount) corrects; blank otherwise. */
public record ClaimRequest(String amount, String date, String note, String corrects) {
}
