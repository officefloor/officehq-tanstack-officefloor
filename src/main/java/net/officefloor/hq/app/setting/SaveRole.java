package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class SaveRole {
    public void save(@Val UserRole role, SettingRepository repository) {
        repository.saveRole(role);
    }
}
