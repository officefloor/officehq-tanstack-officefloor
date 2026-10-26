package net.officefloor.hq.app.setting;

/** The sheet's settings as the screen sees them, each dated one as it is in force on the sheet's today: the clerk limit with two decimals, "" when there is none, the floor with two decimals, the terms in days, the late percent with two decimals, and the policy date as yyyy-MM-dd, "" when there is none (the sheet's today is), and the period numbering from-date as yyyy-MM-dd, "" when it is off, and the terms in force for each grade, in days (its own, or the sheet's terms when it has none), and the cap with two decimals, "" when there is none. {@code dated} is every value recorded for each dated setting, by setting key, by from-date. {@code perms} is the roles each action is permitted to, by action, as a comma separated list, "" for the standing rule. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap, java.util.Map<String, java.util.List<Settings.DatedValue>> dated,
        java.util.Map<String, String> perms) {
}
