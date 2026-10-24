package net.officefloor.hq.app.role;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Gives the user name the role on the sheet, replacing the one it had. */
public class ApplyRole {
    public void apply(@Val RoleInput input, @Val Sheet sheet, RoleRepository repository, Out<Role> roleOut) {
        Role role = repository.findBySheetIdAndUserName(sheet.getId(), input.user())
                .orElseGet(() -> new Role(sheet.getId(), input.user(), input.role()));
        role.setRole(input.role());
        roleOut.set(repository.saveAndFlush(role));
    }
}
