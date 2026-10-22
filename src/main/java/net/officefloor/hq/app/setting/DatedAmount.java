package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A dated setting's figure as it applies to a date: the value in force on it. */
@FunctionalInterface
public interface DatedAmount {
    BigDecimal on(LocalDate date);

    /** The same figure on every date. */
    static DatedAmount always(BigDecimal value) {
        return date -> value;
    }
}
