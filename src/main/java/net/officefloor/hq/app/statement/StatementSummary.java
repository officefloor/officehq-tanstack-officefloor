package net.officefloor.hq.app.statement;

/** A person's statements as the screen shows them: how many were taken, and the last one's due-total and date
 *  (yyyy-MM-dd). */
public record StatementSummary(long personId, int count, String dueTotal, String date) {
}
