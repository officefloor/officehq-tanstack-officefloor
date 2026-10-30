package net.officefloor.hq.app.rollup;

import java.util.Map;

/** The sheet's figures at one moment: each person's (by id, in the order they were added) and the
 *  sheet's totals, all as shown ("n/a" for a due-total with no figure). */
public record RollupFigures(Map<Long, PersonFigures> people, String claimedTotal, String dueTotal) {

    public record PersonFigures(String name, String claimed, String dueTotal) {
    }
}
