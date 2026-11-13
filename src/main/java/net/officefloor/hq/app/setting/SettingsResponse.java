package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings as the screen reads them: the clerk limit (null: no limit), the floor, the terms in days, the terms set for
 * each grade that has its own (by grade), the late percent, the policy date ("": blank, the sheet's today), the period
 * numbering from date ("": off), the entry date ("": off), the cap ("": none), the proof threshold ("": none), the note threshold ("": none) — the dated ones as in force on the sheet's today — each role given,
 * by user name, the permission matrix (by action, the roles allowed it; "": blank, the standing rule), and every dated value recorded, by key, oldest from-date first (value "": blank), the grace days ("": off), the steward role delegated, in the order made, the pot ("": none) and the parent sheet id ("": none).
 */
public record SettingsResponse(String clerkLimit, String floor, int terms, Map<String, Integer> gradeTerms, String latePct, String policyDate, String periodNumberingFrom, String entryDate, String cap, String proofThreshold, String noteThreshold, List<RoleResponse> roles,
        Map<String, List<FromValueResponse>> history, Map<String, String> permissions, String graceDays,
        List<DelegationResponse> delegations, String pot, String parent) {

    public record RoleResponse(String user, String role) {
    }

    public record DelegationResponse(String user, String from, String to, String by) {
    }

    public record FromValueResponse(String from, String value) {
    }

    public static SettingsResponse from(Settings settings) {
        BigDecimal noteThreshold = settings.dated().noteThresholdOn(settings.today());
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
                settings.roles().stream().map((r) -> new RoleResponse(r.user(), r.role().label())).toList(),
                history,
                settings.permissions().entries(),
                settings.graceDays() == null ? "" : settings.graceDays().toString(),
                settings.delegations().stream().map((d) -> new DelegationResponse(d.user(), d.from().toString(),
                        d.to().toString(), d.by())).toList(),
                settings.pot() == null ? "" : settings.pot().toPlainString(),
                settings.parent() == null ? "" : settings.parent());
    }
}
