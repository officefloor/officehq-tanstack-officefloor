package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveCap {
    public void save(@Val Cap cap, @Val Sheet sheet, SettingRepository repository) {
        if (cap.given()) {
            repository.saveDated(sheet.id(), DatedSettings.CAP, DatedSettings.fromOr(cap.from(), sheet.today()),
                    cap.value() == null ? null : cap.value().toPlainString());
        }
    }
}
