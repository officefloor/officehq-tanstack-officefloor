package net.officefloor.hq.app.rollup;

import java.util.Map;

/** The sheet's figures at one moment: each person's claimed and due-total (by person id, in the order
 *  they were added) and the sheet's claimed-total and due-total, all as shown (two decimals, n/a). */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal) {

    /** A person's figures; {@code name} is what the ROLLUP line shows, not part of the comparison. */
    public record Figures(String name, String claimed, String dueTotal) {
        boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal);
        }
    }
}
