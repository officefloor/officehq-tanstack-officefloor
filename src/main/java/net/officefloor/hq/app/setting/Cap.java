package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated cap with two decimals; {@code value} null: no cap. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record Cap(boolean given, BigDecimal value, LocalDate from) {

    /** The due-total capped: the cap covers it plus the cut total, so it is at most the cap less the cut total, not below 0.00 (none: as it is). */
    public static BigDecimal apply(BigDecimal cap, BigDecimal cutTotal, BigDecimal dueTotal) {
        if (cap == null) {
            return dueTotal;
        }
        BigDecimal room = cap.subtract(cutTotal).max(BigDecimal.ZERO).setScale(2);
        return dueTotal.compareTo(room) <= 0 ? dueTotal : room;
    }
}
