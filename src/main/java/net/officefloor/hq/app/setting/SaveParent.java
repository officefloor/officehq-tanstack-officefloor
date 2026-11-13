package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveParent {
    public void save(@Val Parent parent, @Val Sheet sheet, SettingRepository repository) {
        if (parent.given()) {
            repository.saveParent(sheet.id(), parent.value());
        }
    }
}
