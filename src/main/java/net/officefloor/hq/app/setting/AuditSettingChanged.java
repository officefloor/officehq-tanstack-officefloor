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
        if (input.setsProofThreshold()) {
            audit.record(head + "proof-threshold value=" + SettingsResponse.text(input.proofThreshold())
                    + from(input, "proof-threshold"));
        }
        if (input.setsNoteThreshold()) {
            audit.record(head + "note-threshold value=" + SettingsResponse.text(input.noteThreshold())
                    + from(input, "note-threshold"));
        }
        if (input.setsGraceDays()) {
            audit.record(head + "grace-days value=" + (input.graceDays() != null ? input.graceDays() : ""));
        }
        if (input.setsEntryDate()) {
            audit.record(head + "entry-date value=" + (input.entryDate() != null ? input.entryDate() : ""));
        }
        input.gradeGrace().forEach((grade, days) -> audit.record(head + "grace-" + grade + " value="
                + (days != null ? days : "") + from(input, "grace-" + grade)));
        input.gradeProofThreshold().forEach((grade, amount) -> audit.record(head + "proof-threshold-" + grade + " value="
                + SettingsResponse.text(amount) + from(input, "proof-threshold-" + grade)));
        if (input.setsPot()) {
            audit.record(head + "pot value=" + SettingsResponse.text(input.pot()));
        }
        if (input.setsParent()) {
            audit.record(head + "parent value=" + (input.parent() != null ? input.parent() : ""));
        }
        if (input.fiscalStartMonth() != null) {
            audit.record(head + "fiscal-start-month value=" + input.fiscalStartMonth());
        }
        if (input.setsAlertThreshold()) {
            audit.record(head + "alert-threshold value=" + SettingsResponse.text(input.alertThreshold()));
        }
        if (input.setsRounding()) {
            audit.record(head + "rounding value=" + (input.rounding() != null ? input.rounding() : "")
                    + from(input, SettingHistory.ROUNDING));
        }
        if (input.setsReviewThreshold()) {
            audit.record(head + "review-threshold value=" + SettingsResponse.text(input.reviewThreshold())
                    + from(input, "review-threshold"));
        }
        if (input.setsEscalateAfter()) {
            audit.record(head + "escalate-after value=" + (input.escalateAfter() != null ? input.escalateAfter() : ""));
        }
        if (input.setsReviewSla()) {
            audit.record(head + "review-sla value=" + (input.reviewSla() != null ? input.reviewSla() : ""));
        }
        if (input.setsProofKinds()) {
            audit.record(head + "proof-kinds value=" + (input.proofKinds() != null ? input.proofKinds() : ""));
        }
        if (input.setsDefaultGrade()) {
            audit.record(head + "default-grade value=" + (input.defaultGrade() != null ? input.defaultGrade() : ""));
        }
        input.perms().forEach((action, roles) -> audit.record(head + "perm-" + action + " value=" + roles));
    }

    private static String from(SettingsInput input, String key) {
        var from = input.from().get(key);
        return from != null ? " from=" + from : "";
    }
}
