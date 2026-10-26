package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The sheet's figures at one moment: each person's (by id, in list order), and the sheet's
 * claimed-total, due-total (everyone's due-total added up, skipping those that are n/a) and overdue-total.
 */
public record RollupSnapshot(Map<Long, Figures> people, BigDecimal claimedTotal, BigDecimal dueTotal,
        BigDecimal overdueTotal) {
}
