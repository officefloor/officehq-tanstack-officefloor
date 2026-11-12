package net.officefloor.hq.app.statement;

/** A person's statements as the front end reads them: how many were taken, and the last one's
 *  due-total (two decimals, or n/a) and date (yyyy-MM-dd); both null when none was taken. */
public record StatementSummary(int count, String lastDueTotal, String lastDate) {
    public static final StatementSummary NONE = new StatementSummary(0, null, null);
}
