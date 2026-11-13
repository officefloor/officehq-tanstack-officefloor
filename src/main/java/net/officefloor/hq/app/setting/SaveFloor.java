package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveFloor {
    public void save(@Val Floor floor, @Val Sheet sheet, SettingRepository repository) {
        if (floor.value() != null) {
            repository.saveFloor(sheet.id(), floor.value());
        }
    }
}
