package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidSetting setting, @Val RollupFigures before,
            Settings settings, Sheets sheets, Rollup rollup, Audit audit, ObjectResponse<SettingsResponse> response) {
        if (setting.roles() != null) {
            settings.setPermission(sheetId, setting.key().substring(ValidateSetting.PERM.length()), setting.roles());
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + setting.roles());
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        if (ValidateSetting.POLICY_DATE.equals(setting.key())
                || ValidateSetting.PERIOD_NUMBERING_FROM.equals(setting.key())) {
            if (setting.roles() != null) {
            settings.setPermission(sheetId, setting.key().substring(ValidateSetting.PERM.length()), setting.roles());
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + setting.roles());
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        if (ValidateSetting.POLICY_DATE.equals(setting.key())) {
                settings.setPolicyDate(sheetId, setting.date());
            } else {
                settings.setPeriodNumberingFrom(sheetId, setting.date());
            }
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + (setting.date() == null ? "" : setting.date().toString()));
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        if (ValidateSetting.ENTRY_DATE.equals(setting.key())) {
            settings.setEntryDate(sheetId, setting.date());
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + (setting.date() == null ? "" : setting.date().toString()));
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        if (ValidateSetting.GRACE_DAYS.equals(setting.key())) {
            settings.setGraceDays(sheetId, setting.value() == null ? null : setting.value().intValue());
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + (setting.value() == null ? "" : setting.value().toPlainString()));
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        if (ValidateSetting.POT.equals(setting.key())) {
            settings.setPot(sheetId, setting.value());
            audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                    + " key=" + setting.key() + " value=" + (setting.value() == null ? "" : setting.value().toPlainString()));
            rollup.recordChanges(sheetId, setting.user(), before);
            response.send(settings.load(sheetId));
            return;
        }
        // Recorded from its from-date (the sheet's today when none was given), keeping the values recorded from other dates.
        settings.setFrom(sheetId, setting.key(), setting.from() != null ? setting.from() : sheets.today(sheetId), setting.value());
        audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                + " key=" + setting.key() + " value=" + (setting.value() == null ? "" : setting.value().toPlainString())
                + (setting.from() == null ? "" : " from=" + setting.from()));
        rollup.recordChanges(sheetId, setting.user(), before);
        response.send(settings.load(sheetId));
    }
}
