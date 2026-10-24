package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A validated settings form: whether the clerk limit is set and to what (two decimals; null: no limit), and
 *  whether the floor is set and to what (two decimals), and whether the terms are set and to what (days), and whether the late percent is set and to what (two decimals),
 *  and whether the policy date is set and to what (null: the sheet's today),
 *  and whether the period numbering from is set and to what (null: off),
 *  and the grades whose own terms are set, by grade (A, B, C, in that order), to what (days; null: the sheet's terms). */
public record SettingsInput(boolean setClerkLimit, BigDecimal clerkLimit, boolean setFloor, BigDecimal floor,
        boolean setTerms, int terms, boolean setLatePct, BigDecimal latePct,
        boolean setPolicyDate, LocalDate policyDate,
        boolean setPeriodNumberingFrom, LocalDate periodNumberingFrom,
        Map<String, Integer> gradeTerms) {
}
