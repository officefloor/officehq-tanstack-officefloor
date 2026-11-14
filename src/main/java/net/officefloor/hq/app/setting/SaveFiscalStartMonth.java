package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveFiscalStartMonth {
    public void save(@Val FiscalStartMonth month, @Val Sheet sheet, SettingRepository repository) {
        if (month.given()) {
            repository.saveFiscalStartMonth(sheet.id(), month.value());
        }
    }
}
