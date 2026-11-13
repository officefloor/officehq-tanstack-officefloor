package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGradeProofThreshold {
    public void save(@Val GradeProofThreshold threshold, @Val Sheet sheet, SettingRepository repository) {
        threshold.values().forEach((grade, value) -> repository.saveDated(sheet.id(), DatedSettings.gradeProofThreshold(grade),
                DatedSettings.fromOr(threshold.fromOf(grade), sheet.today()), value == null ? null : value.toPlainString()));
    }
}
