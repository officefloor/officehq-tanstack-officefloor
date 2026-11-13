package net.officefloor.hq.app.rollup;

import java.util.Map;

/**
 * A sheet's figures at one moment: each person's claimed and due-total (by person id, in the order they were
 * added) and the sheet's claimed-total and due-total, each as shown (two decimals; a due-total may be n/a).
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal) {

    /** A person's name and figures. */
    public record Figures(String name, String claimed, String dueTotal) {

        /** Whether the figures are the same as the other's, whatever the name. */
        public boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal);
        }
    }
}
