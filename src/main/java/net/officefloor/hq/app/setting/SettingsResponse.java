package net.officefloor.hq.app.setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The settings as the screen shows them: the clerk limit with two decimals ('' when there is none) and the floor
 *  with two decimals and the terms in days and the late percent with two decimals and the policy date (yyyy-mm-dd; '' when blank) and the period numbering from (yyyy-mm-dd; '' when off) and each grade's own
 *  terms in days ('' when it has none: it uses the sheet's terms) and the cap with two decimals ('' when there is none) and the proof threshold with two decimals ('' when there is none), all as in force on the sheet's today; and every value
 *  recorded for each dated setting, by key, oldest from-date first; and the permission matrix, by action ('' when blank); and the grace days ('' when off); and the note threshold with two decimals ('' when there is none), in force on the sheet's today; and the entry date (yyyy-mm-dd; '' when off); and each grade's grace in days ('' when it has none) and each grade's own proof threshold with two decimals ('' when it has none: it uses the sheet's), in force on the sheet's today; and the pot with two decimals ('' when there is none; not dated); and the parent sheet id ('' when there is none; not dated); and the fiscal year start month, 1 to 12 (not dated); and the alert threshold with two decimals ('' when off; not dated); and the rounding mode, half-up or half-even ('' when none), in force on the sheet's today; and the review threshold with two decimals ('' when there is none), in force on the sheet's today; and the escalation gap in days ('' when off; not dated); and the review SLA in days ('' when none; not dated); and the proof kinds ('' when none; not dated); and the default grade ('' when blank, A; not dated); and each grade's own clerk limit with two decimals ('' when it has none: it uses the sheet's), in force on the sheet's today. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct,
        String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms, String cap,
        Map<String, List<Dated>> dated, String proofThreshold, Map<String, String> perms, String graceDays, String noteThreshold,
        String entryDate, Map<String, String> gradeGrace, Map<String, String> gradeProofThreshold, String pot, String parent,
        String fiscalStartMonth, String alertThreshold, String rounding, String reviewThreshold, String escalateAfter, String reviewSla, String proofKinds,
        String defaultGrade, Map<String, String> gradeClerkLimit) {

    /** A dated setting's value ('' when blank) and the date (yyyy-mm-dd) it is in force from. */
    public record Dated(String from, String value) {
    }

    public static SettingsResponse from(Settings s) {
        return new SettingsResponse(SettingsResponse.text(s.clerkLimit()), SettingsResponse.text(s.floor()),
                String.valueOf(s.terms()), SettingsResponse.text(s.latePct()),
                s.policyDate() != null ? s.policyDate().toString() : "",
                s.periodNumberingFrom() != null ? s.periodNumberingFrom().toString() : "", gradeTerms(s),
                SettingsResponse.text(s.cap()), dated(s), SettingsResponse.text(s.proofThreshold()),
                s.permissions().entries(), s.graceDays() != null ? String.valueOf(s.graceDays()) : "",
                SettingsResponse.text(s.noteThreshold()), s.entryDate() != null ? s.entryDate().toString() : "",
                byGrade(s.gradeGrace()), amountsByGrade(s.gradeProofThreshold()),
                SettingsResponse.text(s.pot()), s.parent() != null ? s.parent() : "",
                String.valueOf(s.fiscalStartMonth()), SettingsResponse.text(s.alertThreshold()),
                s.rounding() != null ? s.rounding() : "", SettingsResponse.text(s.reviewThreshold()),
                s.escalateAfter() != null ? String.valueOf(s.escalateAfter()) : "",
                s.reviewSla() != null ? String.valueOf(s.reviewSla()) : "", s.proofKinds() != null ? s.proofKinds() : "",
                s.defaultGrade() != null ? s.defaultGrade() : "", amountsByGrade(s.gradeClerkLimit()));
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
        return byGrade(s.gradeTerms());
    }

    private static Map<String, String> byGrade(Map<String, Integer> days) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            Integer d = days.get(grade);
            shown.put(grade, d != null ? String.valueOf(d) : "");
        }
        return shown;
    }

    private static Map<String, String> amountsByGrade(Map<String, java.math.BigDecimal> amounts) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            shown.put(grade, SettingsResponse.text(amounts.get(grade)));
        }
        return shown;
    }

    static String text(java.math.BigDecimal limit) {
        return limit != null ? limit.toPlainString() : "";
    }
}
