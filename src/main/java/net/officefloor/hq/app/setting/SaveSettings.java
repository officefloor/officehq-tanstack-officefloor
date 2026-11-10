package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Saves the settings being changed, leaving the others as they are: a figure setting's value is
 * recorded from its from-date (the sheet's today when none is given), keeping the values recorded
 * from other dates and replacing one recorded from the same date. An action's entry in the
 * permission matrix is not dated: it replaces the one before.
 */
public class SaveSettings {
    public void save(@Val SettingsInput input, @Val Sheet sheet, SettingsRepository repository,
            Out<Settings> settingsOut) {
        Settings now = repository.find(sheet.id());
        if (input.policyDateChanged() || input.periodNumberingFromChanged() || input.graceDaysChanged()) {
            repository.saveUndated(new Settings(sheet.id(), now.today(), now.dated(),
                    input.policyDateChanged() ? input.policyDate() : now.policyDate(),
                    input.periodNumberingFromChanged() ? input.periodNumberingFrom() : now.periodNumberingFrom(), now.perms(),
                    input.graceDaysChanged() ? input.graceDays() : now.graceDays()));
        }
        input.perms().forEach((action, roles) -> repository.recordPerm(sheet.id(), action, roles));
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>(input.datedValues());
        values.putAll(input.gradeTermsValues());
        if (input.capValue() != null) values.put(Settings.CAP, input.capValue());
        if (input.proofThresholdValue() != null) values.put(Settings.PROOF_THRESHOLD, input.proofThresholdValue());
        values.forEach((key, value) -> {
            LocalDate from = input.from().getOrDefault(key, sheet.today());
            repository.record(sheet.id(), key, from, value);
        });
        settingsOut.set(repository.find(sheet.id()));
    }
}
