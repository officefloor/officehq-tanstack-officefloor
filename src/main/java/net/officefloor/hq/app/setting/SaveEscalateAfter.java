package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveEscalateAfter {
    public void save(@Val EscalateAfter days, @Val Sheet sheet, SettingRepository repository) {
        if (days.given()) {
            repository.saveEscalateAfter(sheet.id(), days.value());
        }
    }
}
