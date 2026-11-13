package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveClerkLimit {
    public void save(@Val ClerkLimit limit, @Val Sheet sheet, SettingRepository repository) {
        repository.saveClerkLimit(sheet.id(), limit.value());
    }
}
