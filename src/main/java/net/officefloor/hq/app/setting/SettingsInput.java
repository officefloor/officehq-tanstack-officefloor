package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated settings, each scaled to two decimals: whether the clerk limit is changed and to what
 * (null: no limit), the floor (null: left as it is) the terms in days (null: left as they are) and the late percent (null: left as it is),
 * and whether the policy date is changed and to what (null: the sheet's today), and whether the
 * period numbering from is changed and to what (null: off).
 */
public record SettingsInput(boolean clerkLimitChanged, BigDecimal clerkLimit, BigDecimal floor, Integer terms, BigDecimal latePct,
        boolean policyDateChanged, LocalDate policyDate,
        boolean periodNumberingFromChanged, LocalDate periodNumberingFrom) {
}
