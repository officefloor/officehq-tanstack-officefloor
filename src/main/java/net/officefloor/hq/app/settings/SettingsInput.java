package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit), the floor scaled to two decimals the terms in days and the late percent scaled to two decimals and the policy date (null when blank), and the period numbering from-date (null when blank: off). */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor, boolean setsTerms,
        Integer terms, boolean setsLatePct, BigDecimal latePct,
        boolean setsPolicyDate, LocalDate policyDate, boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom) {
}
