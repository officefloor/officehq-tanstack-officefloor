package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit), the floor scaled to two decimals the terms in days and the late percent scaled to two decimals and the policy date (null when blank), and the period numbering from-date (null when blank: off), and the grade terms entered, by grade (in grade order; null
 *  when blank: the grade then uses the sheet's terms), and the cap scaled to two decimals (null for no cap). */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor, boolean setsTerms,
        Integer terms, boolean setsLatePct, BigDecimal latePct,
        boolean setsPolicyDate, LocalDate policyDate, boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom,
        java.util.Map<String, Integer> gradeTerms, boolean setsCap, BigDecimal cap) {
}
