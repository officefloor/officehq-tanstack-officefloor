package net.officefloor.hq.app.setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The settings as the screen shows them: the clerk limit with two decimals ('' when there is none) and the floor
 *  with two decimals and the terms in days and the late percent with two decimals and the policy date (yyyy-mm-dd; '' when blank) and the period numbering from (yyyy-mm-dd; '' when off) and each grade's own
 *  terms in days ('' when it has none: it uses the sheet's terms) and the cap with two decimals ('' when there is none) and the proof threshold with two decimals ('' when there is none), all as in force on the sheet's today; and every value
 *  recorded for each dated setting, by key, oldest from-date first. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct,
        String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms, String cap,
        Map<String, List<Dated>> dated, String proofThreshold) {

    /** A dated setting's value ('' when blank) and the date (yyyy-mm-dd) it is in force from. */
    public record Dated(String from, String value) {
    }

    public static SettingsResponse from(Settings s) {
        return new SettingsResponse(SettingsResponse.text(s.clerkLimit()), SettingsResponse.text(s.floor()),
                String.valueOf(s.terms()), SettingsResponse.text(s.latePct()),
                s.policyDate() != null ? s.policyDate().toString() : "",
                s.periodNumberingFrom() != null ? s.periodNumberingFrom().toString() : "", gradeTerms(s),
                SettingsResponse.text(s.cap()), dated(s), SettingsResponse.text(s.proofThreshold()));
    }

    private static Map<String, List<Dated>> dated(Settings s) {
        Map<String, List<Dated>> dated = new LinkedHashMap<>();
        for (String key : SettingHistory.KEYS) {
            List<Dated> values = new ArrayList<>();
            s.history().of(key).forEach((from, value) -> values.add(new Dated(from.toString(), value != null ? value : "")));
            dated.put(key, values);
        }
        return dated;
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
