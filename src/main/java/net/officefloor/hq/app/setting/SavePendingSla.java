package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SavePendingSla {
    public void save(@Val PendingSla days, @Val Sheet sheet, SettingRepository repository) {
        if (days.given()) {
            repository.savePendingSla(sheet.id(), days.value());
        }
    }
}
