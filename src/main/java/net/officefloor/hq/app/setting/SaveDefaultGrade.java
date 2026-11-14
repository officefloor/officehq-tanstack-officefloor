package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveDefaultGrade {
    public void save(@Val DefaultGrade grade, @Val Sheet sheet, SettingRepository repository) {
        if (grade.given()) {
            repository.saveDefaultGrade(sheet.id(), grade.value());
        }
    }
}
