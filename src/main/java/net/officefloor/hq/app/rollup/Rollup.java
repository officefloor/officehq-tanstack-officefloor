package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A sheet's figures at one moment: each person's claimed, due-total and overdue-total (by person id, in the
 * order of the list) and the sheet's claimed-total, due-total and overdue-total, each as shown (two decimals; a due-total may be n/a),
 * and the sheet's alert threshold (null: off).
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal,
        BigDecimal alertThreshold) {

    /** Whether the due-total as shown (n/a or null: none) is above the threshold (null: off). */
    public static boolean above(String dueTotal, BigDecimal threshold) {
        return threshold != null && dueTotal != null && !dueTotal.equals("n/a")
                && new BigDecimal(dueTotal).compareTo(threshold) > 0;
    }

    /** A person's name and figures, and their score (as the screen shows it; not a figure a rollup line reports). */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal, String score) {

        /** Whether the figures are the same as the other's, whatever the name. */
        public boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }
}
