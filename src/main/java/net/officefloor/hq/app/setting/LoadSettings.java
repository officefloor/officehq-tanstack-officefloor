package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadSettings {
    public void load(@Val Sheet sheet, SettingRepository repository, Out<Settings> settings) {
        settings.set(repository.find(sheet.id()));
    }
}
