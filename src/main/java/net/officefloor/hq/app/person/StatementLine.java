package net.officefloor.hq.app.person;

/** One of a person's statements still standing: its date (yyyy-mm-dd) and its due-total (as shown: two
 *  decimals, or 'n/a'). */
public record StatementLine(String date, String dueTotal) {
}
