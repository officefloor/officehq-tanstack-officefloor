package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A validated settings save: who is making it, and the settings it changes, in order — whether it sets the clerk
 *  limit and to what (two decimals; null: no limit), and the floor it sets (two decimals; null: left as it is) and the terms it sets (days; null: left as it is) and the late percent it sets (two decimals; null: left as it is), and whether it sets the policy date and to what (null: blank, the sheet's today), and whether it sets the period numbering from and to what (null: blank, off), and the grades whose own terms it sets
 *  (A, B, C order) and to what (days; null: none, the sheet's terms), and whether it sets the cap and to what (two
 *  decimals; null: none). */
public record SettingsInput(String user, boolean setsClerkLimit, BigDecimal clerkLimit, BigDecimal floor,
        Integer terms, BigDecimal latePct, boolean setsPolicyDate, LocalDate policyDate,
        boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom,
        Map<String, Integer> gradeTerms, boolean setsCap, BigDecimal cap) {
}
