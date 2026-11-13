package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGradeTerms {
    public void save(@Val GradeTerms terms, @Val Sheet sheet, SettingRepository repository) {
        terms.values().forEach((grade, value) -> repository.saveGradeTerms(sheet.id(), grade, value));
    }
}
