package net.officefloor.hq.app.setting;

import java.util.List;
import java.util.Map;

/**
 * The sheet's settings as the screen shows them: the clerk limit with two decimals, or "" for no limit; and the floor
 * with two decimals; and the terms, a whole number of days; and the late percent with two decimals; and the policy date
 * (yyyy-MM-dd), or "" for the sheet's today; and the period numbering from-date (yyyy-MM-dd), or "" for off; and each grade's own terms (a whole number of days), or "" when it takes the sheet's; and the cap with two decimals, or "" for no cap; and the proof threshold with two decimals, or "" for none; and each action's permitted roles (comma separated), or "" for its standing rule. Each
 * is the value in force on the sheet's today. And, for each dated key, its values as recorded, by from-date. And the grace days (a whole number), or "" for off. And the note threshold
 * with two decimals, or "" for none. And the entry date (yyyy-MM-dd), or "" for off. And each grade's grace (a whole number of days), or "" for none. And each grade's
 * own proof threshold with two decimals, or "" for none (the grade takes the sheet's).
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, String termsA, String termsB, String termsC, String cap,
        String proofThreshold, Map<String, String> perms, Map<String, List<DatedValue>> dated,
        String graceDays, String noteThreshold, String entryDate, String graceA, String graceB, String graceC,
        String proofThresholdA, String proofThresholdB, String proofThresholdC) {
}
