package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.Map;

/** Every person's figures on a sheet at one moment, keyed by person id in the order they were
 *  added, and the sheet's totals. A due-total is null when it is n/a. An overdue-total is the dues of
 *  the counting OPEN (or PENDING) claims whose due-by is before the sheet's today (n/a ones skipped). A
 *  score is the person's counting SETTLED claims' figures (corrections left out) over 100, less 5 for each counting overdue
 *  claim, rounded half to even to a whole number, as the screen shows it. */
public record Snapshot(Map<Long, PersonFigures> people, BigDecimal claimedTotal, BigDecimal dueTotal, BigDecimal overdueTotal) {

    public record PersonFigures(String name, BigDecimal claimed, BigDecimal dueTotal, BigDecimal overdueTotal, BigDecimal score) {
    }
}
