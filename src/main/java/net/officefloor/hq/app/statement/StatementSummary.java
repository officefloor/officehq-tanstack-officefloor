package net.officefloor.hq.app.statement;

import java.util.List;

/** A person's statements as the front end reads them: how many were taken, and the last one's
 *  due-total (two decimals, or n/a) and date (yyyy-MM-dd); both null when none was taken; and every
 *  statement still standing, in the order they were taken. */
public record StatementSummary(int count, String lastDueTotal, String lastDate, List<Line> lines) {
    public static final StatementSummary NONE = new StatementSummary(0, null, null, List.of());

    /** A statement's date (yyyy-MM-dd) and its due-total (two decimals, or n/a). */
    public record Line(String date, String dueTotal) {
    }
}
