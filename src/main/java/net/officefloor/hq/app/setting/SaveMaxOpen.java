package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveMaxOpen {
    public void save(@Val MaxOpen max, @Val Sheet sheet, SettingRepository repository) {
        if (max.given()) {
            repository.saveMaxOpen(sheet.id(), max.value());
        }
    }
}
