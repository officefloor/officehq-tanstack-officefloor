package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * The settings as the form submits them; a setting left out (null) is left as it is. The clerk limit has two
 * decimals at most (blank: no limit); the floor has two decimals at most (blank: back to 1.00); the terms are
 * whole days, 0 or more (blank: back to 14); the late percent has two decimals at most (blank: back to 0.00); the policy date is
 * yyyy-MM-dd (blank: the sheet's today); the period numbering from date is yyyy-MM-dd (blank: off); the entry date is yyyy-MM-dd (blank: off); the cap has two decimals at most (blank: no cap); the proof threshold has two decimals at most (blank: none); the note threshold has two decimals at most (blank: none); the review threshold has two decimals at most (blank: none). The grade terms are
 * whole days, 0 or more, by grade, each only for a grade it carries (blank: none of its own, the sheet's terms).
 * The clerk limit, floor, terms, late percent, cap, proof threshold, note threshold, review threshold and each grade's terms are recorded from their from-date, yyyy-MM-dd
 * (left out or blank: the sheet's today). The permissions are, by action (open, settle, void, reassign, close, reopen),
 * the roles allowed it, comma separated (blank: the standing rule); they are not dated. The grace days are whole days,
 * 0 or more (blank: off); they are not dated. The grade grace days are whole days, 0 or more, by grade, each only for a
 * grade it carries (blank: none), recorded from their from-date by grade (left out or blank: the sheet's today). The pot has two decimals at most (blank: none); it is not dated. The parent is a sheet id (blank: none); it is not dated. The fiscal start month is 1 to 12 (blank: back to 1); it is not dated. The alert threshold has two decimals at most (blank: off); it is not dated. The grade
 * proof thresholds have two decimals at most, by grade, each only for a grade it carries (blank: none, the sheet's), recorded
 * from their from-date by grade (left out or blank: the sheet's today). The rounding mode is half-up or half-even (blank: none,
 * the policy date decides), recorded from its from-date (left out or blank: the sheet's today). The escalation gap is whole
 * days, 0 or more (blank: off); it is not dated. The review SLA is whole days, 0 or more (blank: none); it is not dated.
 * The proof kinds are receipt or photo, comma separated (blank: none); they are not dated.
 */
public record SettingsRequest(@Size(max = 30) String clerkLimit, @Size(max = 30) String floor, @Size(max = 30) String terms,
        @Size(max = 30) String latePct, @Size(max = 30) String policyDate, @Size(max = 30) String periodNumberingFrom, @Size(max = 30) String entryDate, @Size(max = 30) String cap,
        @Size(max = 30) String proofThreshold, @Size(max = 30) String noteThreshold, @Size(max = 30) String reviewThreshold,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeTerms,
        @Size(max = 30) String clerkLimitFrom, @Size(max = 30) String floorFrom, @Size(max = 30) String termsFrom,
        @Size(max = 30) String latePctFrom, @Size(max = 30) String capFrom, @Size(max = 30) String proofThresholdFrom,
        @Size(max = 30) String noteThresholdFrom, @Size(max = 30) String reviewThresholdFrom,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeTermsFrom,
        @Size(max = 10) Map<String, @Size(max = 100) String> permissions, @Size(max = 30) String graceDays,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeGrace,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeGraceFrom,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeProofThreshold,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeProofThresholdFrom,
        @Size(max = 30) String pot, @Size(max = 100) String parent, @Size(max = 30) String fiscalStartMonth,
        @Size(max = 30) String alertThreshold, @Size(max = 30) String rounding, @Size(max = 30) String roundingFrom,
        @Size(max = 30) String escalateAfter, @Size(max = 30) String reviewSla, @Size(max = 100) String proofKinds) {
}
