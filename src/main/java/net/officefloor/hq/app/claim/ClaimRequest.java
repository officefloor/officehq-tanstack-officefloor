package net.officefloor.hq.app.claim;

/**
 * A claim as the screen sends it, to add or to edit: an amount (0.00 or more, up to two
 * decimals), a date (yyyy-MM-dd, or blank for the sheet's today) and a note (may be empty).
 */
public record ClaimRequest(String amount, String date, String note) {
}
