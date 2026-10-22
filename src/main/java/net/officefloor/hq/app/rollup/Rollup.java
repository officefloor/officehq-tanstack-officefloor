package net.officefloor.hq.app.rollup;

import java.util.Map;

/** The sheet's figures at one moment: each person's claimed, due-total and overdue-total by id, in the order they were added,
 *  and the sheet's claimed-total, due-total and overdue-total, all as the screen shows them (two decimals, or n/a). */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {

    /** A person's figures: their name, claimed, due-total and overdue-total. */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal) {
    }
}
