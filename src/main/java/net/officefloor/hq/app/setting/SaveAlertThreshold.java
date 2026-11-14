package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveAlertThreshold {
    public void save(@Val AlertThreshold threshold, @Val Sheet sheet, SettingRepository repository) {
        if (threshold.given()) {
            repository.saveAlertThreshold(sheet.id(), threshold.value());
        }
    }
}
