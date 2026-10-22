package net.officefloor.hq.app.setting;

import java.util.LinkedHashMap;
import java.util.Map;

/** The settings as the screen shows them: the clerk limit with two decimals ('' when there is none) and the floor
 *  with two decimals and the terms in days and the late percent with two decimals and the policy date (yyyy-mm-dd; '' when blank) and the period numbering from (yyyy-mm-dd; '' when off) and each grade's own
 *  terms in days ('' when it has none: it uses the sheet's terms). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct,
        String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms) {
    public static SettingsResponse from(Settings s) {
        return new SettingsResponse(SettingsResponse.text(s.clerkLimit()), SettingsResponse.text(s.floor()),
                String.valueOf(s.terms()), SettingsResponse.text(s.latePct()),
                s.policyDate() != null ? s.policyDate().toString() : "",
                s.periodNumberingFrom() != null ? s.periodNumberingFrom().toString() : "", gradeTerms(s));
    }

    private static Map<String, String> gradeTerms(Settings s) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            Integer days = s.gradeTerms().get(grade);
            terms.put(grade, days != null ? String.valueOf(days) : "");
        }
        return terms;
    }

    static String text(java.math.BigDecimal limit) {
        return limit != null ? limit.toPlainString() : "";
    }
}
