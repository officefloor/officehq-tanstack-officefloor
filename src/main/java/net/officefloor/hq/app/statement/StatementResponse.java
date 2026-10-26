package net.officefloor.hq.app.statement;

/**
 * A person's statements as the screen sees them: how many were taken, and the last one's due-total and
 * date as yyyy-MM-dd (both null when none was taken).
 */
public record StatementResponse(long personId, int count, String dueTotal, String date) {
}
