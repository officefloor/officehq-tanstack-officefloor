package net.officefloor.hq.app.setting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings as the screen reads them: the clerk limit (null: no limit), the floor, the terms in days, the terms set for
 * each grade that has its own (by grade), the late percent, the policy date ("": blank, the sheet's today), the period
 * numbering from date ("": off), the cap ("": none) — the dated ones as in force on the sheet's today — each role given,
 * by user name, and every dated value recorded, by key, oldest from-date first (value "": blank).
 */
public record SettingsResponse(String clerkLimit, String floor, int terms, Map<String, Integer> gradeTerms, String latePct, String policyDate, String periodNumberingFrom, String cap, List<RoleResponse> roles,
        Map<String, List<FromValueResponse>> history) {

    public record RoleResponse(String user, String role) {
    }

    public record FromValueResponse(String from, String value) {
    }

    public static SettingsResponse from(Settings settings) {
        Map<String, List<FromValueResponse>> history = new LinkedHashMap<>();
        settings.dated().history().forEach((key, values) -> history.put(key, values.stream()
                .map((v) -> new FromValueResponse(v.from().toString(), v.value() == null ? "" : v.value())).toList()));
        return new SettingsResponse(
                settings.clerkLimit() == null ? null : settings.clerkLimit().toPlainString(),
                settings.floor().toPlainString(),
                settings.terms(),
                settings.gradeTerms(),
                settings.latePct().toPlainString(),
                settings.policyDate() == null ? "" : settings.policyDate().toString(),
                settings.periodNumberingFrom() == null ? "" : settings.periodNumberingFrom().toString(),
                settings.cap() == null ? "" : settings.cap().toPlainString(),
                settings.roles().stream().map((r) -> new RoleResponse(r.user(), r.role().label())).toList(),
                history);
    }
}
