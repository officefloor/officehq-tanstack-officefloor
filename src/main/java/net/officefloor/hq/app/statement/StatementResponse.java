package net.officefloor.hq.app.statement;

import java.util.List;

/**
 * A person's statements as the screen sees them: how many were taken, and the last one's due-total and
 * date as yyyy-MM-dd (both null when none was taken), and a line per statement still standing, oldest first by date
 * (then in the order taken), as "yyyy-MM-dd due-total".
 */
public record StatementResponse(long personId, int count, String dueTotal, String date, List<String> lines) {
}
