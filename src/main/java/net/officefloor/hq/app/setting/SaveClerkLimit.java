package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveClerkLimit {
    public void save(@Val ClerkLimit limit, @Val Sheet sheet, SettingRepository repository) {
        if (limit.given()) {
            repository.saveDated(sheet.id(), DatedSettings.CLERK_LIMIT, DatedSettings.fromOr(limit.from(), sheet.today()),
                    limit.value() == null ? null : limit.value().toPlainString());
        }
    }
}
