package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings as the screen reads them: the clerk limit (null: no limit), the floor, the terms in days, the terms set for
 * each grade that has its own (by grade), the late percent, the policy date ("": blank, the sheet's today), the period
 * numbering from date ("": off), the entry date ("": off), the cap ("": none), the proof threshold ("": none), the note threshold ("": none), the review threshold ("": none) — the dated ones as in force on the sheet's today — each role given,
 * by user name, the permission matrix (by action, the roles allowed it; "": blank, the standing rule), and every dated value recorded, by key, oldest from-date first (value "": blank), the grace days ("": off), the steward role delegated, in the order made, the pot ("": none), the parent sheet id ("": none) the fiscal year start month (1 to 12), the alert threshold ("": off) and the rounding mode in force on the sheet's today ("": none).
 */
public record SettingsResponse(String clerkLimit, String floor, int terms, Map<String, Integer> gradeTerms, String latePct, String policyDate, String periodNumberingFrom, String entryDate, String cap, String proofThreshold, String noteThreshold, String reviewThreshold, List<RoleResponse> roles,
        Map<String, List<FromValueResponse>> history, Map<String, String> permissions, String graceDays,
        List<DelegationResponse> delegations, String pot, String parent, String fiscalStartMonth, String alertThreshold, String rounding) {

    public record RoleResponse(String user, String role) {
    }

    public record DelegationResponse(String user, String from, String to, String by) {
    }

    public record FromValueResponse(String from, String value) {
    }

    public static SettingsResponse from(Settings settings) {
        BigDecimal noteThreshold = settings.dated().noteThresholdOn(settings.today());
        BigDecimal reviewThreshold = settings.dated().reviewThresholdOn(settings.today());
        String rounding = settings.dated().roundingOn(settings.today());
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
                settings.entryDate() == null ? "" : settings.entryDate().toString(),
                settings.cap() == null ? "" : settings.cap().toPlainString(),
                settings.proofThreshold() == null ? "" : settings.proofThreshold().toPlainString(),
                noteThreshold == null ? "" : noteThreshold.toPlainString(),
                reviewThreshold == null ? "" : reviewThreshold.toPlainString(),
                settings.roles().stream().map((r) -> new RoleResponse(r.user(), r.role().label())).toList(),
                history,
                settings.permissions().entries(),
                settings.graceDays() == null ? "" : settings.graceDays().toString(),
                settings.delegations().stream().map((d) -> new DelegationResponse(d.user(), d.from().toString(),
                        d.to().toString(), d.by())).toList(),
                settings.pot() == null ? "" : settings.pot().toPlainString(),
                settings.parent() == null ? "" : settings.parent(),
                Integer.toString(settings.fiscalStartMonth()),
                settings.alertThreshold() == null ? "" : settings.alertThreshold().toPlainString(),
                rounding == null ? "" : rounding);
    }
}
