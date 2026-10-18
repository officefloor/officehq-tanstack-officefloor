package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class SaveRole {
    public void save(@Val UserRole role, UserRoleRepository repository) {
        repository.saveAndFlush(role);
    }
}
