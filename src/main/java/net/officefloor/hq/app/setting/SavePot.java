package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SavePot {
    public void save(@Val Pot pot, @Val Sheet sheet, SettingRepository repository) {
        if (pot.given()) {
            repository.savePot(sheet.id(), pot.value());
        }
    }
}
