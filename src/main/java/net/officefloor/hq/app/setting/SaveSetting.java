package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidSetting setting, @Val RollupFigures before,
            Settings settings, Rollup rollup, Audit audit, ObjectResponse<SettingsResponse> response) {
        if (ValidateSetting.POLICY_DATE.equals(setting.key())
                || ValidateSetting.PERIOD_NUMBERING_FROM.equals(setting.key())) {
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
        if (ValidateSetting.CAP.equals(setting.key())) {
            settings.setCap(sheetId, setting.value());
        } else if (ValidateSetting.FLOOR.equals(setting.key())) {
            settings.setFloor(sheetId, setting.value());
        } else if (ValidateSetting.LATE_PCT.equals(setting.key())) {
            settings.setLatePct(sheetId, setting.value());
        } else if (ValidateSetting.TERMS.equals(setting.key())) {
            settings.setTerms(sheetId, setting.value().intValueExact());
        } else if (setting.key().startsWith(ValidateSetting.GRADE_TERMS)) {
            settings.setGradeTerms(sheetId, setting.key().substring(ValidateSetting.GRADE_TERMS.length()),
                    setting.value().intValueExact());
        } else {
            settings.setClerkLimit(sheetId, setting.value());
        }
        audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                + " key=" + setting.key() + " value=" + (setting.value() == null ? "" : setting.value().toPlainString()));
        rollup.recordChanges(sheetId, setting.user(), before);
        response.send(settings.load(sheetId));
    }
}
