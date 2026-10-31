package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A validated settings change on a sheet: whether the clerk limit is set and to what (null for no
 *  limit), the floor, the terms and the late percent, each null when it is not being set, and
 *  whether the policy date is set and to what (null for blank: the sheet's today), and whether the period numbering from is set and to what (null for blank: off), and the terms set per grade, by grade (a null value clears the grade's own terms, so it uses the sheet's), and whether the cap is set and to what (null for no cap), and the from-date given for a dated setting, by its key (clerk-limit, floor, terms, terms-<grade>, late-pct, cap; one not in it is from the sheet's today), and whether the proof threshold is set and to what (null for none), and the permissions set, by action (roles comma separated; null for blank), and whether the grace days are set and to what (null for off), and whether the note threshold is set and to what (null for none). */
public record SettingsChange(String sheetId, boolean setsClerkLimit, BigDecimal clerkLimit, BigDecimal floor, Integer terms, BigDecimal latePct, boolean setsPolicyDate, LocalDate policyDate, boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom, Map<String, Integer> gradeTerms, String by, boolean setsCap, BigDecimal cap, Map<String, LocalDate> froms, boolean setsProofThreshold, BigDecimal proofThreshold, Map<String, String> perms, boolean setsGraceDays, Integer graceDays, boolean setsNoteThreshold, BigDecimal noteThreshold) {
}
