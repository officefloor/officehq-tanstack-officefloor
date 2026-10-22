package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A sheet's settings: the clerk limit (two decimals; null: no limit) the floor (two decimals) and the terms
 *  (days; a claim's due-by is its date plus them) and the late percent (two decimals) and the policy date (null: the sheet's today; a claim dated before it rounds
 *  half up, on or after it half to even) and the period numbering from (null: off; on or after it, by the sheet's today, a claim opened
 *  is numbered within its period) and the grades' own terms (days, by grade; a grade without its own uses the sheet's
 *  terms). */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, BigDecimal latePct,
        LocalDate policyDate, LocalDate periodNumberingFrom, Map<String, Integer> gradeTerms) {
}
