package net.officefloor.hq.app.setting;

import java.util.Map;

/** Validated settings to save: the clerk limit with two decimals, or null for no limit; the floor with
 *  two decimals, or null to leave it as it is; the terms in days, or null to leave them as they are;
 *  the late percent with two decimals, or null to leave it as it is; the policy date (yyyy-mm-dd), or null for the sheet's today. The period numbering date
 *  (yyyy-mm-dd), or null/empty for period numbering off. Each grade's own terms to set, by grade, in grade order (a grade not
 *  given is left as it is). */
public record SettingsInput(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        Map<String, String> gradeTerms) {
}
