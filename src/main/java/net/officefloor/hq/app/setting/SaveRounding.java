package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveRounding {
    public void save(@Val RoundingSetting rounding, @Val Sheet sheet, SettingRepository repository) {
        if (rounding.given()) {
            repository.saveDated(sheet.id(), DatedSettings.ROUNDING,
                    DatedSettings.fromOr(rounding.from(), sheet.today()), rounding.value());
        }
    }
}
