package net.officefloor.hq.app.setting;

/**
 * Settings as the screen sends them, each only when it is being changed (null: left as it is): the
 * clerk limit, with up to two decimals (blank: no limit), the floor, with up to two decimals, and
 * the terms, a whole number of days, and the late percent, with up to two decimals, and the policy
 * date as yyyy-MM-dd (blank: the sheet's today), and the period numbering from as yyyy-MM-dd
 * (blank: off), and the terms of each grade being changed, a whole number of days (blank: the
 * sheet's terms), and the cap, with up to two decimals (blank: none), and the from-date of each
 * figure setting being changed, by its key (clerk-limit, floor, terms, late-pct, terms-A, terms-B,
 * terms-C, cap, proof-threshold, note-threshold), as yyyy-MM-dd (blank or missing: the sheet's today), and the proof
 * threshold, with up to two decimals (blank: none), and the entry of each action of the permission
 * matrix being changed, by action (open, settle, void, reassign, close, reopen), a comma separated
 * list of roles, clerk or steward (blank: the standing rule), and the grace days, a whole number of
 * days (blank: off), and the note threshold, with up to two decimals (blank: none), and the entry date as yyyy-MM-dd
 * (blank: off), and the grace days of each grade being changed, a whole number of days (blank:
 * none), dated by its key (grace-A, grace-B, grace-C), and the proof threshold of each grade being
 * changed, with up to two decimals (blank: none, the sheet's), dated by its key (proof-threshold-A,
 * proof-threshold-B, proof-threshold-C), and the pot, with up to two decimals (blank: none), not dated, and the parent sheet's id (blank: none), not dated,
 * and the fiscal year start month, 1 to 12, not dated, and the alert threshold, with up to two decimals
 * (blank: off), not dated, and the rounding mode, half-up or half-even (blank: none), dated by its key (rounding), and the review threshold, with up to two decimals (blank: none), dated by its key
 * (review-threshold), and the escalation gap, a whole number of days (blank: off), not dated, and the review
 * SLA, a whole number of days (blank: none), not dated, and the proof kinds, a comma separated list of
 * kinds, receipt or photo (blank: none), not dated, and the default grade, one of the grades (blank: A), not dated, and the score
 * thresholds, mid and high, whole numbers (blank: none), not dated, and the score log switch, on (blank: off), not dated, and
 * the clerk limit of each grade being changed, with up to two decimals (blank: none, the sheet's), dated by its key
 * (clerk-limit-A, clerk-limit-B, clerk-limit-C), and the maximum of open claims per person, a whole number (blank:
 * none), not dated.
 */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap, java.util.Map<String, String> from,
        String proofThreshold, java.util.Map<String, String> perms, String graceDays, String noteThreshold,
        String entryDate, java.util.Map<String, String> gradeGrace,
        java.util.Map<String, String> gradeProofThreshold, String pot, String parent,
        String fiscalStartMonth, String alertThreshold, String rounding, String reviewThreshold,
        String escalateAfter, String reviewSla, String proofKinds, String defaultGrade,
        String scoreMid, String scoreHigh, String scoreLog,
        java.util.Map<String, String> gradeClerkLimit, String maxOpen, String pendingSla) {
}
