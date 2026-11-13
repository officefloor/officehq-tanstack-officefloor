package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveCap {
    public void save(@Val Cap cap, @Val Sheet sheet, SettingRepository repository) {
        if (cap.given()) {
            repository.saveCap(sheet.id(), cap.value());
        }
    }
}
