package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Saves the settings being changed, leaving the others as they are: a figure setting's value is
 * recorded from its from-date (the sheet's today when none is given), keeping the values recorded
 * from other dates and replacing one recorded from the same date. An action's entry in the
 * permission matrix is not dated: it replaces the one before, nor is the parent sheet, nor the alert threshold, nor the escalation gap, nor the review SLA, nor the proof kinds, nor the default grade, nor the score thresholds, nor the score log switch, nor the maximum of open claims.
 */
public class SaveSettings {
    public void save(@Val SettingsInput input, @Val Sheet sheet, SettingsRepository repository,
            net.officefloor.hq.app.sheet.SheetRepository sheets, Out<Settings> settingsOut) {
        if (input.parentChanged()) sheets.saveParent(sheet.id(), input.parent());
        Settings now = repository.find(sheet.id());
        if (input.policyDateChanged() || input.periodNumberingFromChanged() || input.graceDaysChanged()
                || input.entryDateChanged() || input.potChanged() || input.fiscalStartMonthChanged()
                || input.alertThresholdChanged() || input.escalateAfterChanged() || input.reviewSlaChanged()
                || input.proofKindsChanged() || input.defaultGradeChanged() || input.scoreMidChanged()
                || input.scoreHighChanged() || input.scoreLogChanged() || input.maxOpenChanged()
                || input.pendingSlaChanged()) {
            repository.saveUndated(new Settings(sheet.id(), now.today(), now.dated(),
                    input.policyDateChanged() ? input.policyDate() : now.policyDate(),
                    input.periodNumberingFromChanged() ? input.periodNumberingFrom() : now.periodNumberingFrom(), now.perms(),
                    input.graceDaysChanged() ? input.graceDays() : now.graceDays(),
                    input.entryDateChanged() ? input.entryDate() : now.entryDate(),
                    input.potChanged() ? input.pot() : now.pot(),
                    input.fiscalStartMonthChanged() ? input.fiscalStartMonth() : now.fiscalStartMonth(),
                    input.alertThresholdChanged() ? input.alertThreshold() : now.alertThreshold(),
                    input.escalateAfterChanged() ? input.escalateAfter() : now.escalateAfter(),
                    input.reviewSlaChanged() ? input.reviewSla() : now.reviewSla(),
                    input.proofKindsChanged() ? input.proofKinds() : now.proofKinds(),
                    input.defaultGradeChanged() ? input.defaultGrade() : now.defaultGrade(),
                    input.scoreMidChanged() ? input.scoreMid() : now.scoreMid(),
                    input.scoreHighChanged() ? input.scoreHigh() : now.scoreHigh(),
                    input.scoreLogChanged() ? input.scoreLog() : now.scoreLog(),
                    input.maxOpenChanged() ? input.maxOpen() : now.maxOpen(),
                    input.pendingSlaChanged() ? input.pendingSla() : now.pendingSla()));
        }
        input.perms().forEach((action, roles) -> repository.recordPerm(sheet.id(), action, roles));
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>(input.datedValues());
        values.putAll(input.gradeTermsValues());
        values.putAll(input.gradeGraceValues());
        values.putAll(input.gradeProofThresholdValues());
        values.putAll(input.gradeClerkLimitValues());
        if (input.capValue() != null) values.put(Settings.CAP, input.capValue());
        if (input.proofThresholdValue() != null) values.put(Settings.PROOF_THRESHOLD, input.proofThresholdValue());
        if (input.noteThresholdValue() != null) values.put(Settings.NOTE_THRESHOLD, input.noteThresholdValue());
        if (input.roundingValue() != null) values.put(Settings.ROUNDING, input.roundingValue());
        if (input.reviewThresholdValue() != null) values.put(Settings.REVIEW_THRESHOLD, input.reviewThresholdValue());
        values.forEach((key, value) -> {
            LocalDate from = input.from().getOrDefault(key, sheet.today());
            repository.record(sheet.id(), key, from, value);
        });
        settingsOut.set(repository.find(sheet.id()));
    }
}
