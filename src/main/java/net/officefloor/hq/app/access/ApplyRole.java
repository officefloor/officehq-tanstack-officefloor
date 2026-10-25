package net.officefloor.hq.app.access;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyRole {
    public void apply(@Val SheetResponse sheet, @Val RoleInput input, RoleRepository repository, Out<Role> role) {
        role.set(repository.set(sheet.id(), input.user(), input.role()));
    }
}
