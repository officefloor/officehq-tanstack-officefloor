package net.officefloor.hq.app.claim;

/** Body of a claim add or edit: who is making it, the amount as typed, the date (blank for the
 *  sheet's today), the note and, for a correction, the number of the claim it corrects (blank otherwise), and its
 *  external reference (blank or absent: none). */
public record ClaimRequest(String user, String amount, String date, String note, String corrects, String ref) {
}
