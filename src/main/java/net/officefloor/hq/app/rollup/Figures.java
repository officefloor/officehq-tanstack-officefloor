package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;

/** A person's figures: their claimed and their due-total (null: n/a), both with two decimals. */
public record Figures(String name, BigDecimal claimed, BigDecimal dueTotal) {

    /** Whether the claimed or the due-total differs from {@code other}'s. */
    boolean movedFrom(Figures other) {
        return other == null || !claimed.equals(other.claimed) || !java.util.Objects.equals(dueTotal, other.dueTotal);
    }
}
