package net.officefloor.hq.app.statement;

import java.util.List;

/**
 * A person's statements as the screen shows them: how many have been taken, and the last one's due-total (two
 * decimals, or n/a) and date (ISO yyyy-MM-dd, the date it was taken as of, else the sheet's today when it was taken),
 * and a line per statement standing, "<date> <due-total>", oldest first by date.
 */
public record StatementSummary(long person, int count, String lastDueTotal, String lastDate, List<Line> lines) {

    /** One statement standing: its date and due-total (two decimals, or n/a). */
    public record Line(String date, String dueTotal) {
    }
}
