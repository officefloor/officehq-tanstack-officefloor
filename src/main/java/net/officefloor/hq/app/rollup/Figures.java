package net.officefloor.hq.app.rollup;

import java.util.Map;

/**
 * A sheet's figures at one moment, as the screen shows them: each person's (by id, in the order they were
 * added) and the sheet's totals.
 */
public record Figures(Map<Long, PersonFigures> people, String claimedTotal, String dueTotal, String overdueTotal) {

    /** A person's name, claimed, due-total ({@code "n/a"} when they have none), overdue-total and score. */
    public record PersonFigures(String name, String claimed, String dueTotal, String overdueTotal, String score) {

        public boolean sameAs(PersonFigures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }
}
