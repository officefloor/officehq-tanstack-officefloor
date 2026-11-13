package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated cap with two decimals; {@code value} null: no cap. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record Cap(boolean given, BigDecimal value, LocalDate from) {

    /** The due-total capped: at most the cap (none: as it is). */
    public static BigDecimal apply(BigDecimal cap, BigDecimal dueTotal) {
        return cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }
}
