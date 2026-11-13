package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated cap with two decimals; {@code value} null: no cap. {@code given} false: left as it is. */
public record Cap(boolean given, BigDecimal value) {

    /** The due-total capped: at most the cap (none: as it is). */
    public static BigDecimal apply(BigDecimal cap, BigDecimal dueTotal) {
        return cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }
}
