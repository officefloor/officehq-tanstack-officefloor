package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGradeTerms {
    public void save(@Val GradeTerms terms, @Val Sheet sheet, SettingRepository repository) {
        terms.values().forEach((grade, value) -> repository.saveDated(sheet.id(), DatedSettings.gradeTerms(grade),
                DatedSettings.fromOr(terms.fromOf(grade), sheet.today()), value == null ? null : String.valueOf(value)));
    }
}
