package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveLatePct {
    public void save(@Val LatePct latePct, @Val Sheet sheet, SettingRepository repository) {
        if (latePct.value() != null) {
            repository.saveLatePct(sheet.id(), latePct.value());
        }
    }
}
