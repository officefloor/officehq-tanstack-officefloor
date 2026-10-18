package net.officefloor.hq.app.setting;

import java.util.Map;

/** The body of saving the sheet's settings: the clerk limit (0.00 or more, up to two decimals; empty: no limit)
 *  the floor (0.00 or more, up to two decimals; empty or absent: unchanged) and the terms (a whole number of
 *  days, 0 or more; empty or absent: unchanged) and the late percent (0.00 or more, up to two decimals; empty or
 *  absent: unchanged) and the policy date (yyyy-mm-dd; empty: the sheet's today). The period numbering date
 *  (yyyy-mm-dd), or null/empty for period numbering off. And each grade's own terms, by grade (a whole number of days, 0 or more;
 *  empty or absent: unchanged). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        Map<String, String> gradeTerms) {
}
