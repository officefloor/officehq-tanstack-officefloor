package net.officefloor.hq.app.setting;

import java.util.Map;

/** The sheet's settings as shown: the clerk limit with two decimals, or null for no limit; the floor with two decimals;
 *  the terms in days; the late percent with two decimals; the policy date (yyyy-mm-dd), or null for the sheet's today. The period numbering date
 *  (yyyy-mm-dd), or null/empty for period numbering off. Each grade's own terms in days, by grade (a grade with none is not listed:
 *  the sheet's terms are in force for it). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        Map<String, String> gradeTerms) {
}
