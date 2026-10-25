package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;

/**
 * A person's figures: their claimed, their due-total (null: n/a) and their overdue-total, all with two decimals.
 */
public record Figures(String name, BigDecimal claimed, BigDecimal dueTotal, BigDecimal overdueTotal) {

    /** Whether the claimed, the due-total or the overdue-total differs from {@code other}'s. */
    boolean movedFrom(Figures other) {
        return other == null || !claimed.equals(other.claimed) || !java.util.Objects.equals(dueTotal, other.dueTotal)
                || !overdueTotal.equals(other.overdueTotal);
    }
}
