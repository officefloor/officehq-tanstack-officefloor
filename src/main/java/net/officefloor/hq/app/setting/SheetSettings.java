package net.officefloor.hq.app.setting;

/**
 * The settings of a sheet being saved, each null when it is left as it is: the clerk limit with two decimals,
 * "" for no limit, the floor with two decimals, the terms as a whole number of days, the late percent with two decimals, and the policy date as yyyy-MM-dd, "" for none, and the period numbering from-date as yyyy-MM-dd, "" for off, and the cap, the proof threshold and the note threshold, each with two decimals, "" for none, and the grace days as a whole number of days, "" for off, and the entry date as yyyy-MM-dd, "" for off.
 * {@code gradeTerms} are the terms of a grade's own being saved, as a whole number of days, by grade (a grade left out is left as it is).
 * {@code gradeGrace} are the grace days of a grade being saved, as a whole number of days, "" for none, by grade (a grade left out is left as it is).
 * {@code gradeProof} are the proof thresholds of a grade being saved, with two decimals, "" for none (the sheet's is in force), by grade (a grade left out is left as it is).
 * {@code gradeClerkLimit} are the clerk limits of a grade being saved, with two decimals, "" for none (the sheet's is in force), by grade (a grade left out is left as it is).
 * {@code perms} are the roles each action is permitted to being saved, by action in {@link Settings#PERM_ACTIONS} order, as a comma separated list, "" for the standing rule.
 * {@code pot} is the pot being saved, with two decimals, "" for none (not dated).
 * {@code parent} is the id of the parent sheet being saved, "" for none (not dated).
 * {@code fiscalStartMonth} is the month (1 to 12) the fiscal year starts on being saved (not dated).
 * {@code alertThreshold} is the alert threshold being saved, with two decimals, "" for off (not dated).
 * {@code rounding} is the rounding mode being saved, half-up or half-even, "" for none (dated).
 * {@code reviewThreshold} is the review threshold being saved, with two decimals, "" for none (dated).
 * {@code escalateAfter} is the escalation gap being saved, as a whole number of days, "" for off (not dated).
 * {@code reviewSla} is the review SLA being saved, as a whole number of days, "" for none (not dated).
 * {@code proofKinds} are the proof kinds being saved, receipt or photo, comma separated without repeats, "" for none (not dated).
 * {@code defaultGrade} is the default grade being saved, A, B or C, "" for none (so A; not dated).
 * {@code from} is the from-date given for a dated setting, by setting key (a key left out is saved from the sheet's today, and audited without one).
 */
public record SheetSettings(String sheetId, String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom, String cap, String proofThreshold, String graceDays, String noteThreshold, String entryDate,
        java.util.SortedMap<String, String> gradeTerms, java.util.SortedMap<String, String> gradeGrace, java.util.Map<String, java.time.LocalDate> from,
        java.util.Map<String, String> perms, java.util.SortedMap<String, String> gradeProof, String pot, String parent, String fiscalStartMonth, String alertThreshold, String rounding, String reviewThreshold, String escalateAfter, String reviewSla, String proofKinds, String defaultGrade,
        java.util.SortedMap<String, String> gradeClerkLimit) {
}
