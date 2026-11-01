package net.officefloor.hq.app.setting;

/**
 * The sheet's settings as the screen shows them: the clerk limit with two decimals, or "" for no limit; and the floor
 * with two decimals; and the terms, a whole number of days; and the late percent with two decimals; and the policy date
 * (yyyy-MM-dd), or "" for the sheet's today; and the period numbering from-date (yyyy-MM-dd), or "" for off; and each grade's own terms (a whole number of days), or "" when it takes the sheet's; and the cap with two decimals, or "" for no cap.
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, String termsA, String termsB, String termsC, String cap) {
}
