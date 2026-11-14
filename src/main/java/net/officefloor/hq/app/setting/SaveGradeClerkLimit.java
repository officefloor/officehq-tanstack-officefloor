package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGradeClerkLimit {
    public void save(@Val GradeClerkLimit limit, @Val Sheet sheet, SettingRepository repository) {
        limit.values().forEach((grade, value) -> repository.saveDated(sheet.id(), DatedSettings.gradeClerkLimit(grade),
                DatedSettings.fromOr(limit.fromOf(grade), sheet.today()), value == null ? null : value.toPlainString()));
    }
}
