package net.officefloor.hq.app.statement;

import java.util.List;

/** A person's statements as the screen shows them: how many were taken, the last one's due-total and date
 *  (yyyy-MM-dd), and one line per statement in force, oldest first: "&lt;statement date&gt; &lt;due-total&gt;". */
public record StatementSummary(long personId, int count, String dueTotal, String date, List<String> lines) {
}
