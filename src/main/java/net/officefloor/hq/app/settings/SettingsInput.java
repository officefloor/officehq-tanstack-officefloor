package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit), the floor scaled to two decimals the terms in days and the late percent scaled to two decimals and the policy date (null when blank), and the period numbering from-date (null when blank: off), and the grade terms entered, by grade (in grade order; null
 *  when blank: the grade then uses the sheet's terms), and the cap scaled to two decimals (null for no cap), and the
 *  from-date given for each dated setting saved, by key (one not listed has none: it is saved from the sheet's today), and the proof threshold scaled to two decimals (null for none), and the
 *  permission matrix entries entered, by action (a comma separated list of roles; empty when blank: the standing rule), and the grace days (null when blank: off), and the note threshold
 *  scaled to two decimals (null for none). */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor, boolean setsTerms,
        Integer terms, boolean setsLatePct, BigDecimal latePct,
        boolean setsPolicyDate, LocalDate policyDate, boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom,
        java.util.Map<String, Integer> gradeTerms, boolean setsCap, BigDecimal cap, java.util.Map<String, LocalDate> from,
        boolean setsProofThreshold, BigDecimal proofThreshold, java.util.Map<String, String> permissions,
        boolean setsGraceDays, Integer graceDays, boolean setsNoteThreshold, BigDecimal noteThreshold) {
}
