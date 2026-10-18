package net.officefloor.hq.app.setting;

/** Validated settings to save: the clerk limit with two decimals, or null for no limit; the floor with
 *  two decimals, or null to leave it as it is; the terms in days, or null to leave them as they are;
 *  the late percent with two decimals, or null to leave it as it is; the policy date (yyyy-mm-dd), or null for the sheet's today. The period numbering date
 *  (yyyy-mm-dd), or null/empty for period numbering off. */
public record SettingsInput(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom) {
}
