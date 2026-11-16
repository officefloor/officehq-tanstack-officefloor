package net.officefloor.hq.app.claim;

/** Body of an add or edit of a claim, and who makes it. A blank date takes the sheet's today; corrects is the number of the
 *  claim a correction corrects (blank: none). */
public record ClaimRequest(String amount, String date, String note, String corrects, String by) {
}
