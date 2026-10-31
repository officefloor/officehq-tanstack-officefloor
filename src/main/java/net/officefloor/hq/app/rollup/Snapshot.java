package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.Map;

/** Every person's figures on a sheet at one moment, keyed by person id in the order they were
 *  added, and the sheet's totals. A due-total is null when it is n/a. */
public record Snapshot(Map<Long, PersonFigures> people, BigDecimal claimedTotal, BigDecimal dueTotal) {

    public record PersonFigures(String name, BigDecimal claimed, BigDecimal dueTotal) {
    }
}
