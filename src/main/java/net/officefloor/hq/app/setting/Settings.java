package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A sheet's settings in force on its today: the clerk limit (two decimals; null: no limit) the floor (two
 *  decimals) and the terms (days; a claim's due-by is its date plus them) and the late percent (two decimals) and the
 *  policy date (null: the sheet's today; a claim dated before it rounds half up, on or after it half to even) and the
 *  period numbering from (null: off; on or after it, by the sheet's today, a claim opened is numbered within its
 *  period) and the grades' own terms (days, by grade; a grade without its own uses the sheet's terms) and the cap
 *  (two decimals; null: none; a person's due-total is at most it) and the proof threshold (two decimals; null: none) and the note threshold (two decimals; null: none); and every value of the dated ones; and the permission matrix (not dated); and the grace days (null: off; not dated); and the entry date (null: off; not dated; a new claim given no date is dated it); and the grades' grace (days, by grade; a grade without one has none); and the grades' own proof thresholds (two decimals, by grade; a grade without one uses the sheet's); and the pot (two decimals; null: none; not dated); and the parent sheet (a sheet id; null: none; not dated); and the fiscal year start month (1 to 12; not dated); and the alert threshold (two decimals; null: off; not dated); and the rounding mode (half-up or half-even; null: none); and the review threshold (two decimals; null: none); and the escalation gap (days; null: off; not dated). */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, BigDecimal latePct,
        LocalDate policyDate, LocalDate periodNumberingFrom, Map<String, Integer> gradeTerms, BigDecimal cap,
        BigDecimal proofThreshold, BigDecimal noteThreshold, SettingHistory history, Permissions permissions, Integer graceDays,
        LocalDate entryDate, Map<String, Integer> gradeGrace, Map<String, BigDecimal> gradeProofThreshold, BigDecimal pot,
        String parent, int fiscalStartMonth, BigDecimal alertThreshold, String rounding,
        BigDecimal reviewThreshold, Integer escalateAfter) {
}
