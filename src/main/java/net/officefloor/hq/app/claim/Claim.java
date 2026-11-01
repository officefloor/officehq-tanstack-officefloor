package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd) and note. */
public record Claim(long id, long person, String amount, String date, String note) {
}
