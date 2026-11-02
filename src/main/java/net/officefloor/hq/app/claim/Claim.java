package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd) and note. */
public record Claim(long id, long personId, String amount, String date, String note) {
}
