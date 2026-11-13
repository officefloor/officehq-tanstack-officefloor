package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGradeGrace {
    public void save(@Val GradeGrace grace, @Val Sheet sheet, SettingRepository repository) {
        grace.values().forEach((grade, value) -> repository.saveDated(sheet.id(), DatedSettings.gradeGrace(grade),
                DatedSettings.fromOr(grace.fromOf(grade), sheet.today()), value == null ? null : String.valueOf(value)));
    }
}
