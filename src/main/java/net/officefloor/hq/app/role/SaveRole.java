package net.officefloor.hq.app.role;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveRole {
    public void save(@Val RoleInput input, @Val Sheet sheet, RoleRepository repository, Out<Role> roleOut) {
        Role role = new Role(sheet.id(), input.user(), input.role());
        repository.save(role);
        roleOut.set(role);
    }
}
