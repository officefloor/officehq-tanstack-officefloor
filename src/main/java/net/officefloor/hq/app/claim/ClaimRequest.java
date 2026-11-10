package net.officefloor.hq.app.claim;

/**
 * A claim as the screen sends it, to add or to edit: an amount (up to two decimals; negative only
 * for a correction), a date (yyyy-MM-dd, or blank for the sheet's entry date while it is set, else its today), a note (may be empty)
 * and, for a correction, the number of the settled claim it corrects (blank otherwise), and its
 * external reference (blank for none).
 */
public record ClaimRequest(String amount, String date, String note, String corrects, String ref) {
}
