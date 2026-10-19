package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class SaveSetting {
    public void save(@Val Setting setting, SettingRepository repository) {
        repository.save(setting);
    }
}
