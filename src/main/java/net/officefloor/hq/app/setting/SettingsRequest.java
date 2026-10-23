package net.officefloor.hq.app.setting;

import java.util.Map;

/** Body of a settings save: who is making it and the settings it changes — the clerk limit (blank: no limit) and
 *  the floor and the terms (days) and the late percent and the policy date (yyyy-mm-dd; blank: the sheet's today) and the period numbering from (yyyy-mm-dd; blank: off) and grades' own terms (days by grade;
 *  blank: none, the sheet's terms) and the cap (blank: none) and the proof threshold (blank: none) and the permission matrix (roles by action, comma separated; blank: the standing rule) and the grace days (blank: off) and the note threshold (blank: none) and the entry date (yyyy-mm-dd; blank: off) and grades' grace (days by grade; blank: none) and grades' proof thresholds (amounts by grade; blank: none, the sheet's) and the pot (blank: none) and the parent sheet (a sheet id; blank: none) and the fiscal year start month (1 to 12) and the alert threshold (blank: off) and the rounding mode (half-up or half-even; blank: none) and the review threshold (blank: none) and the escalation gap (days; blank: off) and the review SLA (days; blank: none) and the proof kinds (blank: none) and the default grade (A, B or C; blank: A); one left out (null) is left as it is;
 *  and, by key (clerk-limit, floor, terms, terms-A/B/C, grace-A/B/C, late-pct, cap, proof-threshold, proof-threshold-A/B/C, note-threshold, review-threshold), the from-date (yyyy-mm-dd) a dated one it sets
 *  is recorded from (left out or blank: the sheet's today). */
public record SettingsRequest(String user, String clerkLimit, String floor, String terms, String latePct,
        String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms, String cap,
        Map<String, String> from, String proofThreshold, Map<String, String> perms, String graceDays, String noteThreshold,
        String entryDate, Map<String, String> gradeGrace, Map<String, String> gradeProofThreshold, String pot, String parent,
        String fiscalStartMonth, String alertThreshold, String rounding, String reviewThreshold, String escalateAfter, String reviewSla, String proofKinds,
        String defaultGrade) {
}
