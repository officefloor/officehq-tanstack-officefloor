package net.officefloor.hq.app.setting;

import java.util.List;
import java.util.Map;

/**
 * The sheet's settings as the screen shows them: the clerk limit with two decimals, or "" for no limit; and the floor
 * with two decimals; and the terms, a whole number of days; and the late percent with two decimals; and the policy date
 * (yyyy-MM-dd), or "" for the sheet's today; and the period numbering from-date (yyyy-MM-dd), or "" for off; and each grade's own terms (a whole number of days), or "" when it takes the sheet's; and the cap with two decimals, or "" for no cap; and the proof threshold with two decimals, or "" for none; and each action's permitted roles (comma separated), or "" for its standing rule. Each
 * is the value in force on the sheet's today. And, for each dated key, its values as recorded, by from-date. And the grace days (a whole number), or "" for off. And the note threshold
 * with two decimals, or "" for none. And the entry date (yyyy-MM-dd), or "" for off. And each grade's grace (a whole number of days), or "" for none. And each grade's
 * own proof threshold with two decimals, or "" for none (the grade takes the sheet's). And the pot with two decimals,
 * or "" for none. And the parent sheet's id, or "" for none. And the fiscal start month (1 to 12). And the alert
 * threshold with two decimals, or "" for off. And the rounding mode (half-up or half-even), or "" for none. And the review
 * threshold with two decimals, or "" for none. And the escalation gap (a whole number of days), or "" for off. And the review SLA (a whole number of
 * days), or "" for none. And the proof kinds (comma separated), or "" for none. And the default grade, or "" for A. And each grade's own clerk
 * limit with two decimals, or "" for none (the grade takes the sheet's). And the score thresholds of the MID and HIGH
 * bands (whole numbers), each "" for none. And the score log switch, "on" or "" for off. And the
 * maximum of open claims per person (a whole number), or "" for none. And the pending SLA (a whole number of days), or
 * "" for none. And the from-date of each policy version, in the order made (yyyy-MM-dd, or "" for none: it counts
 * always); the current policy version is their count.
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, String termsA, String termsB, String termsC, String cap,
        String proofThreshold, Map<String, String> perms, Map<String, List<DatedValue>> dated,
        String graceDays, String noteThreshold, String entryDate, String graceA, String graceB, String graceC,
        String proofThresholdA, String proofThresholdB, String proofThresholdC, String pot, String parent,
        String fiscalStartMonth, String alertThreshold, String rounding, String reviewThreshold, String escalateAfter, String reviewSla, String proofKinds, String defaultGrade,
        String clerkLimitA, String clerkLimitB, String clerkLimitC, String scoreMid, String scoreHigh, String scoreLog, String maxOpen, String pendingSla,
        List<String> policyVersions) {
}
