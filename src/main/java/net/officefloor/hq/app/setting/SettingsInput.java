package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated settings, each scaled to two decimals: whether the clerk limit is changed and to what
 * (null: no limit), the floor (null: left as it is) the terms in days (null: left as they are) and the late percent (null: left as it is),
 * and whether the policy date is changed and to what (null: the sheet's today), and whether the
 * period numbering from is changed and to what (null: off), and the grades whose terms are changed,
 * in A, B, C order, and to what (null: the sheet's terms), and whether the cap is changed and to
 * what (null: none), and the from-date given for each figure setting, by its key (none given: the
 * sheet's today), and whether the proof threshold is changed and to what (null: none), and the
 * actions whose entry in the permission matrix is changed, in matrix order, each to the roles that
 * may take it as a comma separated list ('' for the standing rule), and whether the grace days are
 * changed and to what (null: off), and whether the note threshold is changed and to what (null: none), and whether the entry date is
 * changed and to what (null: off), and the grades whose grace days are changed, in A, B, C order,
 * and to what (null: none), and the grades whose proof threshold is changed, in A, B, C order, and
 * to what (null: none), and whether the pot is changed and to what (null: none), and whether the parent sheet is changed
 * and to what id (null: none), and whether the fiscal year start month is changed and to what, and whether the alert threshold is
 * changed and to what (null: off), and whether the rounding mode is changed and to what, half-up or
 * half-even (null: none), dated by its key (rounding), and whether the review threshold is changed and to
 * what (null: none), and whether the escalation gap is changed and to what, in days (null: off), and whether
 * the review SLA is changed and to what, in days (null: none), and whether the proof kinds are changed and
 * to what, a comma separated list of kinds (null: none), and whether the default grade is changed and to
 * what, one of the grades (null: A), and whether each score threshold, mid and high, is changed and to what
 * (null: none), and whether the score log switch is changed and to what (false: off), and the grades whose clerk limit is changed, in A, B, C order, and to
 * what (null: none, the sheet's), and whether the maximum of open claims per person is changed and to what (null: none).
 */
public record SettingsInput(boolean clerkLimitChanged, BigDecimal clerkLimit, BigDecimal floor, Integer terms, BigDecimal latePct,
        boolean policyDateChanged, LocalDate policyDate,
        boolean periodNumberingFromChanged, LocalDate periodNumberingFrom,
        java.util.Map<String, Integer> gradeTerms, boolean capChanged, BigDecimal cap,
        java.util.Map<String, LocalDate> from, boolean proofThresholdChanged, BigDecimal proofThreshold,
        java.util.Map<String, String> perms, boolean graceDaysChanged, Integer graceDays,
        boolean noteThresholdChanged, BigDecimal noteThreshold,
        boolean entryDateChanged, LocalDate entryDate, java.util.Map<String, Integer> gradeGrace,
        java.util.Map<String, BigDecimal> gradeProofThreshold,
        boolean potChanged, BigDecimal pot, boolean parentChanged, String parent,
        boolean fiscalStartMonthChanged, Integer fiscalStartMonth,
        boolean alertThresholdChanged, BigDecimal alertThreshold,
        boolean roundingChanged, String rounding,
        boolean reviewThresholdChanged, BigDecimal reviewThreshold,
        boolean escalateAfterChanged, Integer escalateAfter,
        boolean reviewSlaChanged, Integer reviewSla,
        boolean proofKindsChanged, String proofKinds,
        boolean defaultGradeChanged, String defaultGrade,
        boolean scoreMidChanged, Integer scoreMid, boolean scoreHighChanged, Integer scoreHigh,
        boolean scoreLogChanged, boolean scoreLog,
        java.util.Map<String, BigDecimal> gradeClerkLimit, boolean maxOpenChanged, Integer maxOpen,
        boolean pendingSlaChanged, Integer pendingSla) {

    /**
     * The figure settings being changed, by key in the order they are audited, each with the value
     * it is recorded as ('' for none).
     */
    public java.util.Map<String, String> datedValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        if (clerkLimitChanged) values.put(Settings.CLERK_LIMIT, clerkLimit == null ? "" : clerkLimit.toPlainString());
        if (floor != null) values.put(Settings.FLOOR, floor.toPlainString());
        if (terms != null) values.put(Settings.TERMS, terms.toString());
        if (latePct != null) values.put(Settings.LATE_PCT, latePct.toPlainString());
        return values;
    }

    /** The grades whose terms are changed, by key, each with the value it is recorded as ('' for the sheet's). */
    public java.util.Map<String, String> gradeTermsValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        gradeTerms.forEach((grade, days) -> values.put(Settings.gradeTermsKey(grade), days == null ? "" : days.toString()));
        return values;
    }

    /** The grades whose grace days are changed, by key, each with the value it is recorded as ('' for none). */
    public java.util.Map<String, String> gradeGraceValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        gradeGrace.forEach((grade, days) -> values.put(Settings.gradeGraceKey(grade), days == null ? "" : days.toString()));
        return values;
    }

    /** The grades whose proof threshold is changed, by key, each with the value it is recorded as ('' for none). */
    public java.util.Map<String, String> gradeProofThresholdValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        gradeProofThreshold.forEach((grade, figure) ->
                values.put(Settings.gradeProofThresholdKey(grade), figure == null ? "" : figure.toPlainString()));
        return values;
    }

    /** The grades whose clerk limit is changed, by key, each with the value it is recorded as ('' for none). */
    public java.util.Map<String, String> gradeClerkLimitValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        gradeClerkLimit.forEach((grade, figure) ->
                values.put(Settings.gradeClerkLimitKey(grade), figure == null ? "" : figure.toPlainString()));
        return values;
    }

    /** The cap as it is recorded ('' for none), or null when it is not changed. */
    public String capValue() {
        return !capChanged ? null : cap == null ? "" : cap.toPlainString();
    }

    /** The proof threshold as it is recorded ('' for none), or null when it is not changed. */
    public String proofThresholdValue() {
        return !proofThresholdChanged ? null : proofThreshold == null ? "" : proofThreshold.toPlainString();
    }

    /** The rounding mode as it is recorded ('' for none), or null when it is not changed. */
    public String roundingValue() {
        return !roundingChanged ? null : rounding == null ? "" : rounding;
    }

    /** The review threshold as it is recorded ('' for none), or null when it is not changed. */
    public String reviewThresholdValue() {
        return !reviewThresholdChanged ? null : reviewThreshold == null ? "" : reviewThreshold.toPlainString();
    }

    /** The note threshold as it is recorded ('' for none), or null when it is not changed. */
    public String noteThresholdValue() {
        return !noteThresholdChanged ? null : noteThreshold == null ? "" : noteThreshold.toPlainString();
    }
}
