package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, SETTLED or VOID). */
public record Claim(long id, long personId, String amount, String date, String note, String status) {
}
