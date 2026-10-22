package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record each saved setting in the audit file, a line per setting the save sets, with the value saved, and
 *  from=<date> at its end when a from-date was given. */
public class AuditSettingChanged {
    public void audit(@Val SettingsInput input, @Val Settings settings, Audit audit) {
        String head = "SETTING_CHANGED sheet=" + settings.sheetId() + " by=" + input.user() + " key=";
        if (input.setsClerkLimit()) {
            audit.record(head + "clerk-limit value=" + SettingsResponse.text(input.clerkLimit()) + from(input, "clerk-limit"));
        }
        if (input.floor() != null) {
            audit.record(head + "floor value=" + SettingsResponse.text(input.floor()) + from(input, "floor"));
        }
        if (input.terms() != null) {
            audit.record(head + "terms value=" + input.terms() + from(input, "terms"));
        }
        input.gradeTerms().forEach((grade, days) -> audit.record(head + "terms-" + grade + " value="
                + (days != null ? days : "") + from(input, "terms-" + grade)));
        if (input.latePct() != null) {
            audit.record(head + "late-pct value=" + SettingsResponse.text(input.latePct()) + from(input, "late-pct"));
        }
        if (input.setsPolicyDate()) {
            audit.record(head + "policy-date value=" + (input.policyDate() != null ? input.policyDate() : ""));
        }
        if (input.setsPeriodNumberingFrom()) {
            audit.record(head + "period-numbering-from value="
                    + (input.periodNumberingFrom() != null ? input.periodNumberingFrom() : ""));
        }
        if (input.setsCap()) {
            audit.record(head + "cap value=" + SettingsResponse.text(input.cap()) + from(input, "cap"));
        }
    }

    private static String from(SettingsInput input, String key) {
        var from = input.from().get(key);
        return from != null ? " from=" + from : "";
    }
}
