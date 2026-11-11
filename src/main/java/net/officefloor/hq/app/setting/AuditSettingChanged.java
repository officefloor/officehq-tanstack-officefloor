package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records each setting saved, with the value saved (blank for no clerk limit): the clerk limit, then
 * the floor, then the terms, then the late percent, then the policy date, then the period numbering
 * from, then each grade's terms (blank for the sheet's), then the cap (blank for none), then the proof threshold (blank for none), then the note threshold (blank for none), then each
 * action's entry in the permission matrix (blank for the standing rule, never dated), then the grace
 * days (blank for off, never dated), then the entry date (blank for off, never dated), then each
 * grade's grace days (blank for none), then each grade's proof threshold (blank for none), then the pot
 * (blank for none, never dated), then the parent sheet (blank for none, never dated), then the fiscal start month (never dated), then the alert threshold (blank for off, never dated), then the rounding mode (blank for none), then the review threshold (blank for none), then the escalation gap (blank for off, never dated), then the review SLA (blank for none, never dated), then the proof kinds (blank for none, never dated), then the default grade (blank for A, never dated), then the score thresholds, mid then high (blank for none, never dated), then the maximum of open claims (blank for none, never dated); a figure
 * setting saved from a from-date given ends with it. Runs after every rule has passed, so a refused
 * change writes nothing.
 */
public class AuditSettingChanged {
    public void audit(@Val Sheet sheet, @Val SettingsInput input, @Val Settings settings,
            HttpServletRequest request, Audit audit) {
        String head = "SETTING_CHANGED sheet=" + sheet.id() + " by=" + Audit.user(request);
        input.datedValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        input.gradeClerkLimitValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        if (input.policyDateChanged()) {
            audit.record(head + " key=policy-date value=" + (input.policyDate() == null ? "" : input.policyDate()));
        }
        if (input.periodNumberingFromChanged()) {
            audit.record(head + " key=period-numbering-from value="
                    + (input.periodNumberingFrom() == null ? "" : input.periodNumberingFrom()));
        }
        input.gradeTermsValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        if (input.capValue() != null) audit.record(line(head, Settings.CAP, input.capValue(), input));
        if (input.proofThresholdValue() != null) {
            audit.record(line(head, Settings.PROOF_THRESHOLD, input.proofThresholdValue(), input));
        }
        if (input.noteThresholdValue() != null) {
            audit.record(line(head, Settings.NOTE_THRESHOLD, input.noteThresholdValue(), input));
        }
        input.perms().forEach((action, roles) -> audit.record(head + " key=" + Settings.permKey(action) + " value=" + roles));
        if (input.graceDaysChanged()) {
            audit.record(head + " key=grace-days value=" + (input.graceDays() == null ? "" : input.graceDays()));
        }
        if (input.entryDateChanged()) {
            audit.record(head + " key=entry-date value=" + (input.entryDate() == null ? "" : input.entryDate()));
        }
        input.gradeGraceValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        input.gradeProofThresholdValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        if (input.potChanged()) {
            audit.record(head + " key=pot value=" + (input.pot() == null ? "" : input.pot().toPlainString()));
        }
        if (input.fiscalStartMonthChanged()) {
            audit.record(head + " key=fiscal-start-month value=" + input.fiscalStartMonth());
        }
        if (input.alertThresholdChanged()) {
            audit.record(head + " key=" + Settings.ALERT_THRESHOLD + " value="
                    + (input.alertThreshold() == null ? "" : input.alertThreshold().toPlainString()));
        }
        if (input.roundingValue() != null) audit.record(line(head, Settings.ROUNDING, input.roundingValue(), input));
        if (input.reviewThresholdValue() != null) {
            audit.record(line(head, Settings.REVIEW_THRESHOLD, input.reviewThresholdValue(), input));
        }
        if (input.escalateAfterChanged()) {
            audit.record(head + " key=" + Settings.ESCALATE_AFTER + " value="
                    + (input.escalateAfter() == null ? "" : input.escalateAfter()));
        }
        if (input.reviewSlaChanged()) {
            audit.record(head + " key=" + Settings.REVIEW_SLA + " value="
                    + (input.reviewSla() == null ? "" : input.reviewSla()));
        }
        if (input.proofKindsChanged()) {
            audit.record(head + " key=" + Settings.PROOF_KINDS + " value="
                    + (input.proofKinds() == null ? "" : input.proofKinds()));
        }
        if (input.defaultGradeChanged()) {
            audit.record(head + " key=" + Settings.DEFAULT_GRADE + " value="
                    + (input.defaultGrade() == null ? "" : input.defaultGrade()));
        }
        if (input.scoreMidChanged()) {
            audit.record(head + " key=" + Settings.SCORE_MID + " value="
                    + (input.scoreMid() == null ? "" : input.scoreMid()));
        }
        if (input.scoreHighChanged()) {
            audit.record(head + " key=" + Settings.SCORE_HIGH + " value="
                    + (input.scoreHigh() == null ? "" : input.scoreHigh()));
        }
        if (input.scoreLogChanged()) {
            audit.record(head + " key=" + Settings.SCORE_LOG + " value=" + (input.scoreLog() ? Settings.ON : ""));
        }
        if (input.maxOpenChanged()) {
            audit.record(head + " key=" + Settings.MAX_OPEN + " value=" + (input.maxOpen() == null ? "" : input.maxOpen()));
        }
        if (input.pendingSlaChanged()) {
            audit.record(head + " key=" + Settings.PENDING_SLA + " value="
                    + (input.pendingSla() == null ? "" : input.pendingSla()));
        }
        if (input.parentChanged()) {
            audit.record(head + " key=parent value=" + (input.parent() == null ? "" : input.parent()));
        }
    }

    private static String line(String head, String key, String value, SettingsInput input) {
        LocalDate from = input.from().get(key);
        return head + " key=" + key + " value=" + value + (from == null ? "" : " from=" + from);
    }
}
