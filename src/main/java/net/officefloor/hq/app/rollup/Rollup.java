package net.officefloor.hq.app.rollup;

import java.util.Map;

/** The sheet's figures at one moment: each person's claimed, due-total and overdue-total (by person id, in
 *  list order) and the sheet's claimed-total, due-total and overdue-total, all as shown (two decimals, n/a). */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {

    /** A person's figures; {@code name} is what the ROLLUP line shows, and {@code score} their score as the screen
     *  shows it (settled-total / 100, less 5 per overdue claim, rounded half to even), neither part of the comparison. */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal, String score) {
        boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }
}
