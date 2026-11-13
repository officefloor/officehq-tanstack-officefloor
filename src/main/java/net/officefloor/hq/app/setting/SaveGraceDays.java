package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveGraceDays {
    public void save(@Val GraceDays days, @Val Sheet sheet, SettingRepository repository) {
        if (days.given()) {
            repository.saveGraceDays(sheet.id(), days.value());
        }
    }
}
