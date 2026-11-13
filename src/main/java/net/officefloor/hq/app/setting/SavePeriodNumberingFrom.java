package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SavePeriodNumberingFrom {
    public void save(@Val PeriodNumberingFrom from, @Val Sheet sheet, SettingRepository repository) {
        if (from.given()) {
            repository.savePeriodNumberingFrom(sheet.id(), from.value());
        }
    }
}
