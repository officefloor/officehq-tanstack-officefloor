package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;

public class SaveRole {
    public void save(@Val Role role, RoleRepository repository) {
        repository.save(role);
    }
}
