package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit), the floor scaled to two decimals the terms in days and the late percent scaled to two decimals and the policy date (null when blank), and the period numbering from-date (null when blank: off), and the grade terms entered, by grade (in grade order; null
 *  when blank: the grade then uses the sheet's terms), and the cap scaled to two decimals (null for no cap), and the
 *  from-date given for each dated setting saved, by key (one not listed has none: it is saved from the sheet's today), and the proof threshold scaled to two decimals (null for none), and the
 *  permission matrix entries entered, by action (a comma separated list of roles; empty when blank: the standing rule), and the grace days (null when blank: off), and the note threshold
 *  scaled to two decimals (null for none), and the entry date (null when blank: off), and the grade grace days entered, by grade (in grade order; null when blank: none), and the grade proof thresholds entered, by
 *  grade, scaled to two decimals (in grade order; null when blank: none, the sheet's is then used), and the pot scaled to two decimals
 *  (null for none), and the parent sheet's id (null when blank: none), and the fiscal year start month (1 to 12), and the alert threshold scaled to two decimals (null for off), and the rounding mode (half-up or half-even; null when blank: none), and the review threshold scaled to two decimals (null for none), and the escalation gap in days (null when blank: off), and the review SLA in days (null when blank: none), and the default grade (null when blank: A), and the grade clerk limits entered, by grade, scaled to two decimals (in grade order; null when blank: none, the sheet's is then used), and the score log switch. */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor, boolean setsTerms,
        Integer terms, boolean setsLatePct, BigDecimal latePct,
        boolean setsPolicyDate, LocalDate policyDate, boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom,
        java.util.Map<String, Integer> gradeTerms, boolean setsCap, BigDecimal cap, java.util.Map<String, LocalDate> from,
        boolean setsProofThreshold, BigDecimal proofThreshold, java.util.Map<String, String> permissions,
        boolean setsGraceDays, Integer graceDays, boolean setsNoteThreshold, BigDecimal noteThreshold,
        boolean setsEntryDate, LocalDate entryDate, java.util.Map<String, Integer> gradeGrace,
        java.util.Map<String, BigDecimal> gradeProofThreshold, boolean setsPot, BigDecimal pot,
        boolean setsParent, String parent, boolean setsFiscalStartMonth, Integer fiscalStartMonth,
        boolean setsAlertThreshold, BigDecimal alertThreshold,
        boolean setsRounding, String rounding, boolean setsReviewThreshold, BigDecimal reviewThreshold,
        boolean setsEscalateAfter, Integer escalateAfter, boolean setsReviewSla, Integer reviewSla,
        boolean setsProofKinds, String proofKinds, boolean setsDefaultGrade, String defaultGrade,
        java.util.Map<String, BigDecimal> gradeClerkLimit,
        boolean setsScoreMid, Integer scoreMid, boolean setsScoreHigh, Integer scoreHigh,
        boolean setsScoreLog, boolean scoreLog) {
}
