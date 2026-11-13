package net.officefloor.hq.app.rollup;

import java.util.Map;

/**
 * A sheet's figures at one moment: each person's claimed, due-total and overdue-total (by person id, in the
 * order of the list) and the sheet's claimed-total, due-total and overdue-total, each as shown (two decimals; a due-total may be n/a).
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {

    /** A person's name and figures. */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal) {

        /** Whether the figures are the same as the other's, whatever the name. */
        public boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }
}
