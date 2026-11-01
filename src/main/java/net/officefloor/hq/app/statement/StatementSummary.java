package net.officefloor.hq.app.statement;

/**
 * A person's statements as the screen shows them: how many have been taken, and the last one's due-total (two
 * decimals, or n/a) and date (ISO yyyy-MM-dd, the date it was taken as of, else the sheet's today when it was taken).
 */
public record StatementSummary(long person, int count, String lastDueTotal, String lastDate) {
}
